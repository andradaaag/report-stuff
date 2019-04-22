// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp();


exports.makePoliceman = functions.https.onCall((data, context) => {
    if (context.auth.token.policeman !== true) {
        return {
            error: "Request not authorized. User must be a policeman to fulfill request."
        };
    };
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