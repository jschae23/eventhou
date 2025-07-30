const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

const firebaseConfig = {
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "FIREBASE_URL"
}

/**
 * Init firebase connection.
 * @returns {FirebaseFirestore.Firestore}
 */
function init() {
    if (admin.apps.length === 0) {
        admin.initializeApp(firebaseConfig);
    }
    return admin.firestore();
}

/**
 * Close firebase connection.
 */
function close() {
    admin.app().delete().then().catch((err) => console.log(err.toString()));
}

/**
 * Merge or update a document reference with the specified value.
 */
function mergeOrUpdateDocumentRef(doc, field, updateValue, setValue) {
    doc.update(field, updateValue).then().catch(e =>
        mergeDocumentRef(doc, field, setValue)
    )
}

/**
 * Merge a document reference with the specified value.
 */
function mergeDocumentRef(doc, field, setValue) {
    doc.set({[field]: setValue}, {merge: true})
}

module.exports.init = init
module.exports.close = close
module.exports.mergeDocumentRef = mergeDocumentRef
module.exports.mergeOrUpdateDocumentRef = mergeOrUpdateDocumentRef
