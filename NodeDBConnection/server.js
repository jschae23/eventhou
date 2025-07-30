const fetch = require("node-fetch");
const jsdom = require("jsdom");
const {JSDOM} = jsdom;
const cronjobs = require('./cronjobs');
const firebase = require('./firebase');
const eventHandler = require('./EventHandler');

const regexExtractIdFromUrl = /^[^\d]*(\d+)/;
const futureDaysMax = 14; // Max days ahead
const futureDaysPopularity = 3; // Max days ahead for popularity calculations

const DAY_IN_MILLIS = 86400000

/**
 * The default locations array, updated constantly via firebase.
 * @type {{onlineEvents: boolean, dailyLimit: number, name: string, geopoint: {latitude: number, longitude: number}, id: string, enabled: boolean}[]}
 */
let locations = [
    {
        id: 'Munich',
        name: 'Munich',
        geopoint: {
            latitude: 48.15,
            longitude: 11.5833333
        },
        onlineEvents: true,
        //limit: Number.MAX_SAFE_INTEGER,
        dailyLimit: 25,
        enabled: true
    },
]

/**
 * Get all events from Bandsintwon in specified [futureDaysMax] days from all locations.
 *
 * @returns {Promise<void>}
 */
async function getBitEvents() {
    await updateLocations()
    const firestore = firebase.init();
    try {
        for (let town of locations.filter(t => t.enabled)) {
            console.log(`Process location ${town.name}`);
            const processDate = new Date();
            processDate.setUTCHours(0, 0, 0, 0)
            let eventPerTownCounter = 0;
            let dayCounter = 0;
            let eventPerDayCounter = null;
            let stop = false;
            // sets a counter to retrieve all pages from the Bandsintown-API
            for (let page = 1; ; page++) {
                if (stop) {
                    break;
                }
                // gets the Events based on the selected City
                const response = await fetch(`https://www.bandsintown.com/upcomingEvents?page=${page}&longitude=${town.geopoint.longitude}&latitude=${town.geopoint.latitude}`);
                let events = (await response.json()).events;
                events = events.filter(event => town.onlineEvents || event.venueName !== 'Streaming LIVE')

                const batch = firestore.batch();
                // starts an Event iterator
                for (let j = 0; j < events.length; j++, eventPerTownCounter++) {
                    process.stdout.write('Handle event ' + eventPerTownCounter + ' or ' + eventPerDayCounter + " of day\r");
                    const event = events[j];

                    if (event.hasOwnProperty('eventUrl') && event.hasOwnProperty('localStartTime')) {
                        // localStartTime actually represents the UTC date.
                        event.utcDateTimeISO = event.localStartTime;  // can be removed as also redundant, but needs adaption in application
                        event.utcDateTime = event.utcDateTimeISO; // TODO utcDateTime is deprecated or will be replaced with timestamp / date object
                        event.utcDate = new Date(event.utcDateTimeISO);
                        delete event.localStartTime;
                        delete event.eventDate;

                        // use "bit_" as source identifier for Bandsintown
                        event.eventId = 'bit_' + regexExtractIdFromUrl.exec(event.eventUrl)[1];
                        event.artistId = 'bit_' + regexExtractIdFromUrl.exec(event.artistUrl)[1];
                        event.eventSource = "Bandsintown"

                        const eventCollection = firestore.collection('events').doc(processDate.toISOString().substring(0, 10)).collection(town.id)

                        // if event date is in the next [futureDaysMax]
                        while ((processDate.getTime() + DAY_IN_MILLIS) < event.utcDate.getTime()) {
                            processDate.setDate(processDate.getDate() + 1);
                            dayCounter++
                            eventPerDayCounter = null
                            console.log(`Process at ${processDate.toString().substr(0, 10)} in ${town.name} (page: ${page}, dayCounter: ${dayCounter})`);
                        }
                        if (eventPerDayCounter === null) {
                            const eventCollectionSnap = await eventCollection.get()
                            if (!eventCollectionSnap.empty && eventCollectionSnap.size >= town.dailyLimit) {
                                eventPerDayCounter = town.dailyLimit
                            } else {
                                eventPerDayCounter = 0
                            }
                        }
                        if (eventPerDayCounter >= town.dailyLimit) {
                            processDate.setDate(processDate.getDate() + 1);
                            dayCounter++
                            eventPerDayCounter = null
                            process.stdout.write(`Skip previous events at ${processDate.toString().substr(0, 10)} in ${town.name} (dayCounter: ${dayCounter})\r`);
                            continue
                        }
                        if (dayCounter > futureDaysMax) {
                            console.log('Skip next events of ', town.name, ' Today: ' + processDate.toString(), ' Eventday: ' + event.utcDateTimeISO);
                            stop = true;
                            break;
                        }
                        if (event.eventId && processDate.getTime() < event.utcDate.getTime() && eventPerDayCounter < town.dailyLimit) {
                            if (event.eventUrl) {
                                // Fetch extra infos like Genres
                                const eventDetailResponse = await fetch(event.eventUrl);
                                const eventDetailDom = new JSDOM(await eventDetailResponse.text()).window.document;
                                const genres = eventDetailDom.getElementsByClassName("_1v6hYzlTV-hB2ZkAb6CiCv"); // TODO check if this always is the className for genre
                                if (genres && genres.length > 0) {
                                    // Add category music to all events from bandsintown by default
                                    event.categories = [
                                        {
                                            categoryName: 'Music',
                                            subCategoriesName: 'Genre',
                                            subCategories: genres[0].textContent.split(',').map(item => item.replace(/\/|\.|\[|\]|\*|\`/g, ' ').trim()).filter(Boolean),
                                        }
                                    ]
                                }
                            }
                            const eventRef = eventCollection.doc(event.eventId);
                            batch.set(eventRef, event, {merge: true});

                            eventPerDayCounter++
                        }
                    }
                }
                await batch.commit();
            }
        }
    } catch (e) {
        console.error(e);
    } finally {
        firebase.close()
    }
}

/**
 * Normalize the popularity of all events on next [futureDaysPopularity] days.
 */
async function normalizePopularities() {
    const currentDay = new Date();
    currentDay.setUTCHours(0, 0, 0, 0)
    for (let day = 0; day < futureDaysPopularity; day++) {
        const dateStr = currentDay.toISOString().substring(0, 10)
        for (let town of locations.filter(t => t.enabled)) {
            eventHandler.normalizeEventPopularity(dateStr, town.id)
        }
        currentDay.setDate(currentDay.getDate() + 1);
    }
    eventHandler.normalizeEventCategoryPopularity()
}

/**
 *  Gets the saved locations from the Firestore.
 */
async function updateLocations() {
    const firestore = firebase.init()
    const locationCollection = await firestore.collection('locations').get()
    locations = locationCollection.docs.map(doc => {
        return {id: doc.id, ...doc.data()}
    })
}

// Start cronjobs
cronjobs.create(eventHandler.decreaseEventCategoryPopularity, '0 0 1 * * *'); // Once a day, before the new events are fetched
cronjobs.create(getBitEvents, '0 0 2 * * *'); // every day at two o'clock (to ensure the right date) and when starting server
cronjobs.create(normalizePopularities, '0 0 */12 * * *'); // every hour
