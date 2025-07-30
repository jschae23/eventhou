const firebase = require('./firebase');

const USERS_KEY = "users"
const EVENTS_KEY = "events"
const CATEGORIES_KEY = "categories"
const SUBCATEGORIES_KEY = "subCategories"
const POPULARITY_KEY = "popularity"
const POPULARITY_NORMALIZED_KEY = "popularityNormalized"
const USER_FAVORITE_EVENTS = "favoriteEvents"
const POPULARITY_DECREASE = 0.8

const firestore = firebase.init()

/**
 * Normalize the popularity of all events on the specified date and venue.
 */
function normalizeEventPopularity(eventDate, venue) {
    const fbEvents = firestore.collection(EVENTS_KEY)
        .doc(eventDate)
        .collection(venue)
    normalizePopularity(fbEvents)
}

/**
 * Normalize the popularity of a category.
 */
function normalizeEventCategoryPopularity() {
    const fbCategories = firestore.collection(CATEGORIES_KEY)
    fbCategories.get().then(categoriesSnap => {
        categoriesSnap.forEach(category =>
            normalizePopularity(fbCategories.doc(category.id).collection(SUBCATEGORIES_KEY))
        )
    })
    normalizePopularity(fbCategories)
}

/**
 * Generic method to normalize the popularities in a firestore collection.
 */
function normalizePopularity(collection) {
    collection.get().then(collectionSnap => {
        // Overall range of popularity
        let maxPopularity = 0
        collectionSnap.forEach(doc => {
                const pop = doc.get(POPULARITY_KEY)
                if (pop != null && pop > maxPopularity) {
                    maxPopularity = pop
                }
            }
        )
        // Normalized popularity
        collectionSnap.forEach(doc => {
            let pop = doc.get(POPULARITY_KEY)
            if (pop == null) pop = 0
            const popNormalized = pop / maxPopularity
            firebase.mergeOrUpdateDocumentRef(
                collection.doc(doc.id),
                POPULARITY_NORMALIZED_KEY,
                popNormalized,
                popNormalized
            )
        })
    })
}

/**
 * Decrease the categories / subcategories popularity of an event by POPULARITY_DECREASE.
 */
async function decreaseEventCategoryPopularity() {
    const fbCategories = firestore.collection(CATEGORIES_KEY)
    fbCategories.get().then(categoriesSnap => {
        categoriesSnap.forEach(category =>
            decreasePopularity(fbCategories.doc(category.id).collection(SUBCATEGORIES_KEY))
        )
    })
    decreasePopularity(fbCategories)
}

/**
 * Generic method to decrease the popularities by POPULARITY_DECREASE in a firestore collection.
 */
function decreasePopularity(collection) {
    collection.get().then(collectionSnap => {
        collectionSnap.forEach(doc => {
                let pop = doc.get(POPULARITY_KEY)
                if (pop != null) {
                    const updatedPopularity = Math.trunc(pop * POPULARITY_DECREASE)
                    firebase.mergeOrUpdateDocumentRef(
                        collection.doc(doc.id),
                        POPULARITY_KEY,
                        updatedPopularity,
                        updatedPopularity
                    )
                }
            }
        )
    })
}

module.exports.decreaseEventCategoryPopularity = decreaseEventCategoryPopularity
module.exports.normalizeEventPopularity = normalizeEventPopularity
module.exports.normalizeEventCategoryPopularity = normalizeEventCategoryPopularity
