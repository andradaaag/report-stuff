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

                    let tokens = [];
                    let emails = [newReport.activeUsers[0]];
                    emails.push("gaeandrada@gmail.com");

                    // Get tokens
                    snapshot.forEach(doc => {
                        let official = doc.data();
                        console.log(doc.id, '=>', official);

                        //TODO: Compare locations

                        tokens.push(official.fcmToken);
                        // emails.push(official.email); TODO: add email to official object

                    });

                    // Construct notification
                    const payload = {
                        data: {
                            reportId: context.params.reportId,
                            location: "",
                            // location: newReport.latestLocation,
                            citizenName: newReport.citizenName
                        }
                    };

                    console.log("Payload: ", payload);

                    // Send notifications
                    admin.messaging().sendToDevice(tokens, payload)
                        .then((response) => {
                            // if (response.failureCount > 0) {
                            //     const failedTokens = [];
                            //     response.responses.forEach((resp, idx) => {
                            //         if (!resp.success) {
                            //             failedTokens.push(registrationTokens[idx]);
                            //         }
                            //     });
                            //     console.log('List of tokens that caused failures: ' + failedTokens);
                            // }
                            // Response is a message ID string.
                            console.log('Successfully sent message:', response);
                        })
                        .catch((error) => {
                            console.log('Error sending message:', error);
                        });

                    console.log("Officials emails: ", emails);
                    admin.firestore().collection("reports").doc(context.params.reportId).update({"activeUsers": emails});
                }
            );
        }
    );