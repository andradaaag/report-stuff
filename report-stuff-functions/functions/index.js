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
        return false;
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
        return false;
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
        return false;
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

exports.sendInitialNotificationToPolicemen = functions.firestore.document('reports/{reportId}')
    .onCreate(async (snap, context) => {
        const newReport = snap.data();
        const citizenEmail = newReport.activeUsers[0];
        const citizenLocation = newReport.latestLocation;
        const citizenName = newReport.citizenName;
        const radius = 5000; // Radius of 5km (or 5000m)
        const reportId = context.params.reportId;
        const role = "policeman";

        return sendNotificationToRoleNearby(citizenEmail, citizenLocation, citizenName, radius, reportId, role)
    });

exports.sendNotificationToOtherOfficials = functions.firestore.document('reports/{reportId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const newMessage = snap.data();
        const citizenEmail = newMessage.email;
        const citizenLocation = newMessage.location;
        const citizenName = newMessage.name;
        const radius = 5000; // Radius of 5km (or 5000m)
        const reportId = context.params.reportId;
        const roles = await determineRoles(newMessage.mediaType, newMessage.mediaUrl, newMessage.text);

        const promises = [];
        roles.forEach(role => {
            promises.push(sendNotificationToRoleNearby(citizenEmail, citizenLocation, citizenName, radius, reportId, role))
        });
        return Promise.all(promises);
    });

async function determineRoles(mediaType, mediaUrl, text) {
    let roles = [];
    if (mediaType === "text") {
        //TODO: handle text to determine role
        if (text.includes("fire"))
            roles.push("firefighter");
    } else if (mediaType === "image") {
        //TODO: handle image to determine role
    } else if (mediaType === "video") {
        //TODO: handle video to determine role
    } else if (mediaType === "audio") {
        //TODO: handle audio to determine role
    }
    return roles;
}

async function sendNotificationToRoleNearby(email, location, name, radius, reportId, role) {
    const isOfficial = await checkUserIsOfficial(email);
    console.log(isOfficial);
    if (isOfficial)
        return void callback();

    const data = await getOfficialsNearby(location, role, radius);
    await addOfficialToActiveUsersListOfReport(data, email, reportId);
    const address = await convertLocationToAddress(location);
    return sendNotification(address, data, reportId, name);
}

async function getOfficialsNearby(location, role, radius) {
    let snapshot;
    try {
        snapshot = await admin.firestore().collection("officials").get();
    } catch (err) {
        console.log("Error getting officials nearby with role: ", err);
    }
    if (snapshot.empty) {
        console.log('No matching documents.');
        return [];
    }
    const citizenLatitude = location._latitude;
    const citizenLongitude = location._longitude;

    console.log("Citizen latitude: ", citizenLatitude);
    console.log("Citizen longitude: ", citizenLongitude);

    let locationSearchData = [];

    // Prepare data set for location search
    snapshot.forEach(doc => {
        let official = doc.data();
        console.log("Official: ", doc.id, '=>', official);

        if (official.role === role)
            locationSearchData.push({
                _latitude: official.location._latitude,
                _longitude: official.location._longitude,
                email_token: official.email + " " + official.fcmToken
            });
    });

    // Set up geo-nearby for searching in radius
    const Geo = require('geo-nearby');
    const geo = new Geo(locationSearchData, {
        setOptions: {
            id: 'email_token',
            lat: '_latitude',
            lon: '_longitude'
        }
    });

    const data = geo.nearBy(citizenLatitude, citizenLongitude, radius);
    console.log("Officials nearby: ", data);
    return data;
}

async function addOfficialToActiveUsersListOfReport(data, citizenEmail, reportId) {
    let emails = [citizenEmail];
    data.forEach(d => {
        let email_token = d['i'].split(" ");
        emails.push(email_token[0]);
    });

    // Add officials to activeUsers list of the report
    console.log("Officials emails: ", emails);
    let result;
    try {
        result = await admin.firestore().collection("reports").doc(reportId).update({"activeUsers": emails});
    } catch (err) {
        console.log("Error adding official to activeUsers list of report: ", err);
    }
    return result;
}

async function convertLocationToAddress(location) {
    const NodeGeocoder = require('node-geocoder');
    const options = {
        provider: 'google',
        apiKey: 'AIzaSyAedapjo4fcYde13Biu-6DFF47vBRwV2jw',
        formatter: 'string %S %n'
    };

    let response;
    const geocoder = NodeGeocoder(options);
    try {
        response = await geocoder.reverse({
            lat: location._latitude,
            lon: location._longitude
        });
    } catch (err) {
        console.log("Error converting location to address", err)
    }
    return response;
}

async function sendNotification(res, data, reportId, citizenName) {
    let tokens = [];
    data.forEach(d => {
        let email_token = d['i'].split(" ");
        tokens.push(email_token[1]);
    });

    const location = res[0].formattedAddress;
    const payload = {
        notification: {
            title: "New Report from " + citizenName,
            body: location
        },
        data: {
            reportId: reportId,
            location: location,
            citizenName: citizenName
        }
    };
    console.log("Payload: ", payload);

    let response;
    try {
        response = await admin.messaging().sendToDevice(tokens, payload);
        console.log('Successfully sent message:', response);
    } catch (err) {
        console.log("Error sending message: ", err);
        return err;
    }
    return response;
}