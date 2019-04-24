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
        const newReport = {
            "lastTime": newMessage.time,
            "lastLocation": newMessage.location
        };

        // Update report with newMessage.location and timestamp
        console.log("Updating report", reportId, "with latest location and timestamp", newReport);
        return admin.firestore().collection("reports").doc(reportId).update(newReport);
    });