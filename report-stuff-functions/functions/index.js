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

exports.sendNotificationToOtherOfficials = functions.firestore.document('reports/{reportId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const newMessage = snap.data();
        const reportId = context.params.reportId;
        const email = newMessage.email;

        const isOfficial = await checkUserIsOfficial(email);
        console.log(isOfficial);
        if (isOfficial)
            return;

        //TODO: Give specific role based on words
        const data = await getOfficialsNearby(newMessage.location, "policeman");
        await addOfficialToActiveUsersListOfReport(data, email, reportId);
        const address = await convertLocationToAddress(newMessage.location);
        return sendNotification(address, data, reportId, newMessage.name);
    });

async function getOfficialsNearby(location, role) {
    try {
        const snapshot = await admin.firestore().collection("officials").get();
        if (snapshot.empty) {
            console.log('No matching documents.');
            return;
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

        // Radius of 5km (or 5000m)
        const data = geo.nearBy(citizenLatitude, citizenLongitude, 5000);
        console.log("Officials nearby: ", data);
        return data;
    } catch (err) {
        console.log("Error getting officials nearby with role: ", err);
    }
}

async function addOfficialToActiveUsersListOfReport(data, citizenEmail, reportId) {
    try {
        let emails = [citizenEmail];

        data.forEach(d => {
            let email_token = d['i'].split(" ");
            emails.push(email_token[0]);
        });

        // Add officials to activeUsers list of the report
        console.log("Officials emails: ", emails);
        return await admin.firestore().collection("reports").doc(reportId).update({"activeUsers": emails});
    } catch (err) {
        console.log("Error adding official to activeUsers list of report: ", err);
    }
}

async function convertLocationToAddress(location) {
    try {
        const NodeGeocoder = require('node-geocoder');
        const options = {
            provider: 'google',
            apiKey: 'AIzaSyAedapjo4fcYde13Biu-6DFF47vBRwV2jw',
            formatter: 'string %S %n'
        };

        const geocoder = NodeGeocoder(options);
        return await geocoder.reverse({
            lat: location._latitude,
            lon: location._longitude
        });
    } catch (err) {
        console.log("Error converting location to address", err)
    }
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

    try {
        const response = await admin.messaging().sendToDevice(tokens, payload);
        console.log('Successfully sent message:', response);
        return response;
    } catch (err) {
        console.log("Error sending message: ", err);
        return err;
    }
}

exports.sendInitialNotificationToPolicemen = functions.firestore.document('reports/{reportId}').onCreate((snap, context) => {
    //TODO: break it into function calls
    const newReport = snap.data();
        return admin.firestore().collection("officials").get().then((snapshot) => {
                if (snapshot.empty) {
                    console.log('No matching documents.');
                    return;
                }
                const citizenLatitude = newReport.latestLocation._latitude;
                const citizenLongitude = newReport.latestLocation._longitude;

                console.log("Citizen latitude: ", citizenLatitude);
                console.log("Citizen longitude: ", citizenLongitude);

                let tokens = [];
                let emails = [newReport.activeUsers[0]];
                let locationSearchData = [];

                // Prepare data set for location search
                snapshot.forEach(doc => {
                    let official = doc.data();
                    console.log(doc.id, '=>', official);

                    if (official.role === "policeman")
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

                // Radius of 5km (or 5000m)
                const data = geo.nearBy(citizenLatitude, citizenLongitude, 5000);
                console.log(data);

                // Get email and fcmToken from policemen nearby
                data.forEach(d => {
                    let email_token = d['i'].split(" ");
                    emails.push(email_token[0]);
                    tokens.push(email_token[1]);
                });

                // Add policeman to activeUsers list of the report
                console.log("Policemen emails: ", emails);
                admin.firestore().collection("reports").doc(context.params.reportId).update({"activeUsers": emails});

                // Convert citizen location to human readable address
                const NodeGeocoder = require('node-geocoder');
                const options = {
                    provider: 'google',
                    apiKey: 'AIzaSyAedapjo4fcYde13Biu-6DFF47vBRwV2jw',
                    formatter: 'string %S %n'
                };

                const geocoder = NodeGeocoder(options);
                return geocoder.reverse({
                    lat: citizenLatitude,
                    lon: citizenLongitude
                }).then(function (res) {

                    // Construct notification
                    const location = res[0].formattedAddress;
                    const payload = {
                        notification: {
                            title: "New Report from " + newReport.citizenName,
                            body: location
                        },
                        data: {
                            reportId: context.params.reportId,
                            location: location,
                            citizenName: newReport.citizenName
                        }
                    };
                    console.log("Payload: ", payload);

                    // Send notifications to policemen
                    return admin.messaging().sendToDevice(tokens, payload)
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
                            return response;
                        })
                        .catch((error) => {
                            console.log('Error sending message:', error);
                        });
                }).catch(function (err) {
                    console.log("Error: ", err);
                });
            }
        );
    }
);