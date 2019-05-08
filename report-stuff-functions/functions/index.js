// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp();


exports.makePoliceman = functions.https.onCall((data, context) => {
    // Commented for development purposes
    // if (context.auth.token.policeman !== true) {
    //     return {
    //         error: "Request not authorized. User must be a policeman to fulfill request."
    //     };
    // }
    const email = data.email;
    return grantPolicemanRole(email).then(() => {
        return {
            result: `Request fulfilled! ${email} is now a policeman.`
        };
    });
});

async function grantPolicemanRole(email) {
    const user = await admin.auth().getUserByEmail(email);
    if (user.customClaims && user.customClaims.policeman === true) {
        return;
    }
    return admin.auth().setCustomUserClaims(user.uid, {
        policeman: true
    });
}

exports.makeFirefighter = functions.https.onCall((data, context) => {
    // Commented for development purposes
    // if (context.auth.token.firefighter !== true) {
    //     return {
    //         error: "Request not authorized. User must be a firefighter to fulfill request."
    //     };
    // }
    const email = data.email;
    return grantFirefighterRole(email).then(() => {
        return {
            result: `Request fulfilled! ${email} is now a firefighter.`
        };
    });
});

async function grantFirefighterRole(email) {
    const user = await admin.auth().getUserByEmail(email);
    if (user.customClaims && user.customClaims.firefighter === true) {
        return;
    }
    return admin.auth().setCustomUserClaims(user.uid, {
        firefighter: true
    });
}

exports.makeSmurd = functions.https.onCall((data, context) => {
    // Commented for development purposes
    // if (context.auth.token.smurd !== true) {
    //     return {
    //         error: "Request not authorized. User must be a smurd to fulfill request."
    //     };
    // }
    const email = data.email;
    return grantSmurdRole(email).then(() => {
        return {
            result: `Request fulfilled! ${email} is now a smurd.`
        };
    });
});

async function grantSmurdRole(email) {
    const user = await admin.auth().getUserByEmail(email);
    if (user.customClaims && user.customClaims.smurd === true) {
        return;
    }
    return admin.auth().setCustomUserClaims(user.uid, {
        smurd: true
    });
}

exports.updateReport = functions.firestore.document('reports/{reportId}/messages/{messageId}')
    .onCreate((snap, context) => {
        const newMessage = snap.data();
        const reportId = context.params.reportId;
        const email = newMessage.email;
        const newReport = {
            "latestTime": newMessage.time,
            "latestLocation": newMessage.location
        };

        return checkUserIsOfficial(email).then((isOfficial) => {
            if (isOfficial) {
                console.log("Did not update location of report since user", email, "is an official");
                return {
                    result: `Did not update location of report since user ${email} is an official.`
                }
            }
            // Otherwise, update report with newMessage.location and timestamp
            console.log("Updating report", reportId, "with latest location and timestamp", newReport);
            return admin.firestore().collection("reports").doc(reportId).update(newReport);
        });
    });

async function checkUserIsOfficial(email) {
    const user = await admin.auth().getUserByEmail(email);
    return (user.customClaims && (
        user.customClaims.policeman === true
        || user.customClaims.firefighter === true
        || user.customClaims.smurd === true
    ));
}

exports.sendNotification = functions.firestore.document('reports/{reportId}')
    .onCreate((snap, context) => {
            const newReport = snap.data();

            //TODO: Determine roles to receive notification

            //TODO: Search for nearby tokens to send notifications to
            return admin.firestore().collection("officials").get().then((snapshot) => {
                    if (snapshot.empty) {
                        console.log('No matching documents.');
                        return;
                    }

                    let activeOfficials = [];
                    snapshot.forEach(doc => {
                        let official = doc.data();
                        console.log(doc.id, '=>', official);

                        //TODO: Compare locations
                        activeOfficials.push(official);
                        console.log("Active officials: ", activeOfficials);

                        // Construct notification
                        const payload = {
                            data: {
                                reportId: context.params.reportId,
                                location: "",
                                // location: newReport.latestLocation,
                                citizenName: newReport.citizenName,
                                time: newReport.latestTime.toString()
                            }
                        };

                        console.log("Payload: ", payload);

                        // Send notifications
                        const emails = ["gaeandrada@gmail.com"];
                        emails.push(newReport.activeUsers.get(0));

                        activeOfficials.forEach(official => {
                            console.log("Official: ", official);
                            console.log("FCM Token: ", official.fcmToken);
                            admin.messaging().sendToDevice(official.fcmToken, payload).catch((exception) => {
                                console.log("Error: ", exception)
                            });
                            // emails.push(official.email); TODO: add email to official object
                        });

                        console.log("Officials emails: ", emails);
                        admin.firestore().collection("reports").doc(context.params.reportId).update({"activeUsers": emails});
                        //TODO: Check not no override citizen email when doing this
                    });
                }
            );
        }
    );