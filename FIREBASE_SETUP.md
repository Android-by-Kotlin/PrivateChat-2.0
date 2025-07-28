# Firebase Setup Instructions for PrivateChat 2.0

## Prerequisites
1. Make sure you have a Firebase project created
2. Ensure Firebase Authentication with Phone Number is enabled
3. Firebase Realtime Database should be enabled

## Firebase Console Setup

### 1. Enable Phone Authentication
1. Go to Firebase Console → Authentication → Sign-in method
2. Enable Phone authentication
3. Add test phone numbers for testing (e.g., +1 650-555-3434, +91 9876543210)

### 2. Configure Realtime Database Rules
For testing, set your database rules to:
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": {
      ".indexOn": ["phoneNumber"]
    }
  }
}
```

### 3. Database Structure
The app expects the following database structure:
```
privatechat-db/
├── users/
│   ├── {userId}/
│   │   ├── userId: "string"
│   │   ├── phoneNumber: "string" (e.g., "+919876543210")
│   │   ├── name: "string"
│   │   ├── status: "string"
│   │   ├── profileImage: "string" (base64 encoded)
│   │   └── chats/
│   │       └── {chatId}/
│   │           ├── userId: "string"
│   │           ├── phoneNumber: "string"
│   │           ├── name: "string"
│   │           └── profileImage: "string"
├── chats/
│   └── {chatId}/
│       └── ... (chat data)
└── messages/
    └── {senderPhone}/
        └── {receiverPhone}/
            └── {messageId}/
                ├── senderPhoneNumber: "string"
                ├── message: "string"
                └── timestamp: number
```

## App Configuration

### 1. google-services.json
Ensure you have downloaded and placed the `google-services.json` file in the `app/` directory.

### 2. Build Configuration
Your app's `build.gradle.kts` should include:
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
}
```

## Testing the App

### 1. Test User Registration
1. Launch the app
2. Enter a test phone number (with country code, e.g., +919876543210)
3. Enter the test OTP (123456 for test numbers)
4. Complete profile setup

### 2. Test User Search
1. Register at least 2 test users
2. From one user account, tap the floating action button
3. Enter the other user's phone number (with country code)
4. The user should appear if they have registered

### 3. Common Issues and Solutions

#### Issue: "No User found with this phone Number"
**Causes:**
- Phone number format mismatch (missing country code)
- User not registered in the database
- Firebase database rules blocking access
- Network connectivity issues

**Solutions:**
1. Ensure phone numbers include country code (e.g., +91 for India, +1 for USA)
2. Check Firebase Console → Realtime Database to verify user exists
3. Check database rules allow authenticated users to read
4. Check internet connection

#### Issue: Authentication fails
**Solutions:**
1. Verify Phone Authentication is enabled in Firebase Console
2. For testing, add test phone numbers with test verification codes
3. Check SHA-1/SHA-256 fingerprints are added to Firebase project

### 4. Debug Mode
To see detailed logs, run:
```bash
adb logcat | grep -E "PhoneAuth|BaseViewModel|FirebaseTest"
```

## Production Considerations

1. Update database rules for better security:
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid || root.child('users').child(auth.uid).child('chats').hasChild($uid)",
        ".write": "$uid === auth.uid"
      }
    },
    "messages": {
      "$sender": {
        "$receiver": {
          ".read": "$sender === auth.phoneNumber || $receiver === auth.phoneNumber",
          ".write": "$sender === auth.phoneNumber"
        }
      }
    }
  }
}
```

2. Enable Firebase App Check for additional security
3. Implement proper error handling and retry mechanisms
4. Add offline persistence:
```kotlin
FirebaseDatabase.getInstance().setPersistenceEnabled(true)
```

## Troubleshooting Commands

Check if users exist in database:
```kotlin
// Add this temporary code to check database
FirebaseDatabase.getInstance().getReference("users")
    .get().addOnSuccessListener { snapshot ->
        Log.d("Debug", "Total users: ${snapshot.childrenCount}")
        snapshot.children.forEach { child ->
            val user = child.getValue(PhoneAuthUser::class.java)
            Log.d("Debug", "User: ${user?.phoneNumber}")
        }
    }
```
