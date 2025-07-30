/**
 * Create a cronjob.
 *
 * @param doStuff The function which should be executed regularly.
 * @param cronTime
 */
function create(doStuff, cronTime) {
    const CronJob = require('cron').CronJob;
    let isRunning = false; // Save state in order to run only one instance at the same time

    // See: https://github.com/kelektiv/node-cron#usage-basic-cron-usage
    const job = new CronJob(
        cronTime,		// cronTime
        async function () { // onTick
            if (isRunning) return;
            isRunning = true;
            console.log('Start ' + doStuff.name);
            try {
                await doStuff();
            } catch (e) {
                console.log(e.toString());
            }
            console.log('End ' + doStuff.name);
            isRunning = false;
        },
        null,        // onComplete
        true,        // startNow
        null,        // timeZone
        null,        // context
        true,        // runOnInit
        0,           // utcOffset
        // unrefTimeout
    );
    //job.start();
}

module.exports.create = create
