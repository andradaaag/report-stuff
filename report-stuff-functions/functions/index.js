// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp();

const bot = {
    email: "botreportstuff@gmail.com",
    name: "Bot Report",
    photoUrl: "https://firebasestorage.googleapis.com/v0/b/reportstuff.appspot.com/o/bot%2Fbot_picture.png?alt=media&token=b486d550-20d4-4547-a592-aa40f5c88592",
};

exports.makeBot = functions.https.onCall((data, context) => {
    const email = data.email;
    return grantBotRole(email).then(() => {
        return {
            result: `Request fulfilled! ${email} is now a bot.`
        };
    });
});

async function grantBotRole(email) {
    const user = await admin.auth().getUserByEmail(email);
    if (user.customClaims && user.customClaims.bot === true) {
        return false;
    }
    return admin.auth().setCustomUserClaims(user.uid, {
        bot: true
    });
}


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
    .onCreate(async (snap, context) => {
        const newMessage = snap.data();
        const email = newMessage.email;
        const reportId = context.params.reportId;

        const isBot = await checkUserIsBot(email);
        const isOfficial = await checkUserIsOfficial(email);

        console.log("IsBot: " + isBot);
        console.log("IsOfficial: " + isOfficial);

        if (isBot)
            return true;
        if (isOfficial)
            return updateReportWithActiveOfficials(reportId, email);
        return updateReportWithLocationAndTimestamp(reportId, newMessage.time, newMessage.location)
    });

async function updateReportWithActiveOfficials(reportId, email) {
    const newReport = {
        activeOfficials: admin.firestore.FieldValue.arrayUnion.apply(null, [email]),
        notifiedOfficials: admin.firestore.FieldValue.arrayRemove.apply(null, [email])
    };
    console.log("Updating report", reportId, "with status, activeOfficials and notifiedOfficials", newReport);
    return admin.firestore().collection("reports").doc(reportId).update(newReport);
}

async function updateReportWithLocationAndTimestamp(reportId, time, location) {
    const newReport = {
        latestTime: time,
        latestLocation: location
    };
    console.log("Updating report", reportId, "with latest location and timestamp", newReport);
    return admin.firestore().collection("reports").doc(reportId).update(newReport);
}

async function checkUserIsOfficial(email) {
    const user = await admin.auth().getUserByEmail(email);
    return (user.customClaims && (
        user.customClaims.policeman === true
        || user.customClaims.firefighter === true
        || user.customClaims.smurd === true
    ));
}

async function checkUserIsBot(email) {
    const user = await admin.auth().getUserByEmail(email);
    return (user.customClaims && user.customClaims.bot === true);
}

exports.sendInitialNotificationToPolicemen = functions.firestore.document('reports/{reportId}')
    .onCreate(async (snap, context) => {
        const newReport = snap.data();
        const citizenEmail = newReport.citizenEmail;

        const isOfficialOrBot = await checkUserIsOfficial(citizenEmail) || await checkUserIsBot(citizenEmail);
        console.log("Is official or bot: " + isOfficialOrBot);
        if (isOfficialOrBot)
            return true;

        const citizenLocation = newReport.latestLocation;
        const citizenName = newReport.citizenName;
        const radius = 5000; // Radius of 5km (or 5000m)
        const reportId = context.params.reportId;
        const role = "policeman";

        const promises = [];
        promises.push(sendBotMessage(reportId, "The nearest policeman teams were notified of your emergency." +
            " Please describe it using text, images, audio or video recordings.", newReport.latestTime));
        promises.push(sendNotificationToRoleNearby(citizenEmail, citizenLocation, citizenName, radius, reportId, role));
        return Promise.all(promises);
    });

exports.sendNotificationToOtherOfficials = functions.firestore.document('reports/{reportId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const newMessage = snap.data();
        const citizenEmail = newMessage.email;

        const isOfficialOrBot = await checkUserIsOfficial(citizenEmail) || await checkUserIsBot(citizenEmail);
        console.log("Is official or bot: " + isOfficialOrBot);
        if (isOfficialOrBot)
            return true;

        const citizenLocation = newMessage.location;
        const citizenName = newMessage.name;
        const radius = 5000; // Radius of 5km (or 5000m)
        const reportId = context.params.reportId;
        const roles = await determineRoles(newMessage.mediaType, newMessage.mediaUrl, newMessage.text);
        console.log("Roles to send notification to: ", roles);

        const promises = [];
        roles.forEach(role => {
            promises.push(sendNotificationToRoleNearby(citizenEmail, citizenLocation, citizenName, radius, reportId, role));
            promises.push(sendBotMessage(reportId, "The nearest " + role + " teams were notified of your emergency.", newMessage.time))
        });
        return Promise.all(promises);
    });

async function determineRoles(mediaType, mediaUrl, text) {
    let roles = [];
    switch (mediaType) {
        case "text":
            roles = handleText(text);
            break;
        case "image":
            roles = await handleImage(mediaUrl);
            break;
        case "video":
            //TODO: handle video to determine role
            break;
        case "audio":
            //TODO: handle audio to determine role
            break;
    }
    return roles;
}

function handleText(text) {
    let roles = [];
    if (text.includes("fire") || text.includes("flame") || text.includes("explosion")) {
        roles.push("firefighter");
        console.log("Found keyword 'fire', calling firefighters");
    }
    if ((text.includes("broke") && text.includes("arm")) || text.includes("doctor") || text.includes("in labour")) {
        roles.push("smurd");
        console.log("Found keywords related to medical assistance, calling smurd");
    }
    return roles;
}

async function handleImage(mediaUrl) {
    const vision = require('@google-cloud/vision');
    const client = new vision.ImageAnnotatorClient();
    const [result] = await client.labelDetection(mediaUrl);

    const labels = result.labelAnnotations;
    let labelDescriptions = [];
    let roles = [];

    labels.forEach(label => {
        const labelDescription = label.description;
        labelDescriptions.push(labelDescription);

        if (labelDescription.includes("Fire") || labelDescription.includes("Flame") || labelDescription.includes("Explosion")) {
            if (!roles.includes("firefighter"))
                roles.push("firefighter");
            console.log("Found keywords related to fire, calling firefighters");
        }

        if (labelDescription.includes("Crash") || labelDescription.includes("Explosion")) {
            if (!roles.includes("smurd"))
                roles.push("smurd");
            console.log("Found keywords related to possible injuries, calling smurd");
        }
    });
    console.log("Labels: ", labelDescriptions);
    return roles;
}

async function sendBotMessage(reportId, text, time) {
    return admin.firestore().collection("reports").doc(reportId).collection("messages").add({
        email: bot.email,
        mediaType: "text",
        name: bot.name,
        photoUrl: bot.photoUrl,
        text: text,
        time: time
    })
}

async function sendNotificationToRoleNearby(email, location, name, radius, reportId, role) {
    const isOfficialOrBot = await checkUserIsOfficial(email) || await checkUserIsBot(email);
    console.log("Is official or bot: " + isOfficialOrBot);
    if (isOfficialOrBot)
        return true;

    const data = await getOfficialsNearby(location, role, radius);
    await addOfficialToNotifiedOfficialsListOfReport(data, reportId);
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

async function addOfficialToNotifiedOfficialsListOfReport(data, reportId) {
    let emails = [];
    data.forEach(d => {
        let email_token = d['i'].split(" ");
        emails.push(email_token[0]);
    });

    emails = emails.filter(item => item !== bot.email);

    // Add officials to notifiedOfficials list of the report
    console.log("Notified officials: ", emails);
    let result;
    try {
        result = await admin.firestore().collection("reports").doc(reportId)
            .update({
                notifiedOfficials: admin.firestore.FieldValue.arrayUnion.apply(null, emails)
            });
    } catch (err) {
        console.log("Error adding official to notifiedOfficials list of report: ", err);
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
        console.log('Send notification response: ', JSON.stringify(response));
    } catch (err) {
        console.log("Error sending message: ", err);
        return err;
    }
    return response;
}