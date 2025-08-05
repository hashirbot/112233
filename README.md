# Battery Repair ERP - Android App

A comprehensive Android application for managing battery repair services, converted from the original Flask web application to use Firebase Realtime Database.

## Features

### Core Functionality
- **User Authentication**: Role-based access (Admin, Shop Staff, Technician)
- **Battery Management**: Register, track, and manage battery repairs
- **Real-time Updates**: Firebase Realtime Database for live data synchronization
- **Status Tracking**: Complete workflow from received to delivered
- **Customer Management**: Store and manage customer information
- **Staff Notes**: Add notes and comments for battery tracking

### User Roles
- **Admin**: Full system access, user management, settings
- **Shop Staff**: Register batteries, manage customers, generate bills
- **Technician**: Update battery status, add repair comments, set prices

### Battery Workflow
1. **Received**: Battery registered in system
2. **Pending**: Under diagnosis/repair
3. **Ready**: Repair completed, ready for pickup
4. **Delivered**: Battery delivered to customer
5. **Returned**: Battery returned to customer
6. **Not Repairable**: Cannot be repaired

## Technology Stack

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Architecture**: MVVM with Repository pattern
- **UI**: Material Design Components
- **Build System**: Gradle with Kotlin DSL

## Firebase Setup

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "battery-repair-erp"
3. Enable Google Analytics (optional)

### 2. Add Android App
1. Click "Add app" and select Android
2. Package name: `com.batteryrepair.erp`
3. Download `google-services.json`
4. Replace the placeholder file in `app/google-services.json`

### 3. Enable Firebase Services
1. **Authentication**:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication
   
2. **Realtime Database**:
   - Go to Realtime Database
   - Create database in test mode
   - Update security rules as needed

### 4. Database Structure
```json
{
  "users": {
    "userId": {
      "id": "userId",
      "username": "admin",
      "fullName": "Administrator",
      "role": "ADMIN",
      "isActive": true,
      "createdAt": 1640995200000
    }
  },
  "customers": {
    "customerId": {
      "id": "customerId",
      "name": "John Doe",
      "mobile": "9876543210",
      "mobileSecondary": "9876543211",
      "createdAt": 1640995200000
    }
  },
  "batteries": {
    "batteryId": {
      "id": "batteryId",
      "batteryId": "BAT0001",
      "customerId": "customerId",
      "batteryType": "Car Battery",
      "voltage": "12V",
      "capacity": "100Ah",
      "status": "RECEIVED",
      "inwardDate": 1640995200000,
      "servicePrice": 500.0,
      "pickupCharge": 50.0,
      "isPickup": true
    }
  },
  "status_history": {
    "historyId": {
      "id": "historyId",
      "batteryId": "batteryId",
      "status": "PENDING",
      "comments": "Started repair work",
      "updatedBy": "userId",
      "updatedAt": 1640995200000
    }
  },
  "staff_notes": {
    "noteId": {
      "id": "noteId",
      "batteryId": "batteryId",
      "note": "Customer called for update",
      "noteType": "FOLLOWUP",
      "createdBy": "userId",
      "createdAt": 1640995200000,
      "isResolved": false
    }
  },
  "settings": {
    "battery_id_prefix": "BAT",
    "battery_id_start": "1",
    "battery_id_padding": "4",
    "shop_name": "Battery Repair Service"
  }
}
```

## Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24 or higher
- Firebase project configured

### Steps
1. **Clone/Download** the project
2. **Open** in Android Studio
3. **Replace** `app/google-services.json` with your Firebase config
4. **Sync** project with Gradle files
5. **Build** and run the application

### Default Users
Create these users in Firebase Authentication:
- **Admin**: `admin@batteryrepair.local` / `admin123`
- **Staff**: `staff@batteryrepair.local` / `staff123`
- **Technician**: `technician@batteryrepair.local` / `tech123`

## Key Features Implemented

### 1. Dashboard
- Real-time statistics display
- Quick action buttons
- Revenue tracking
- Status overview

### 2. Battery Registration
- Customer information capture
- Battery specifications
- Pickup service options
- Auto-generated battery IDs

### 3. Technician Panel
- Pending battery list
- Status update functionality
- Service price setting
- Comment system

### 4. Real-time Synchronization
- Live data updates across devices
- Offline support with Firebase persistence
- Automatic conflict resolution

### 5. Material Design UI
- Modern Android design patterns
- Responsive layouts
- Intuitive navigation
- Consistent theming

## Architecture

### Repository Pattern
- `FirebaseRepository`: Handles all Firebase operations
- Centralized data access layer
- Error handling and result wrapping

### MVVM Structure
- Activities handle UI and user interactions
- Repository manages data operations
- Models represent data structures

### Firebase Integration
- Real-time listeners for live updates
- Offline persistence enabled
- Optimistic updates for better UX

## Security Considerations

### Firebase Rules
Update Realtime Database rules for production:
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": {
      "$uid": {
        ".write": "$uid === auth.uid || root.child('users').child(auth.uid).child('role').val() === 'ADMIN'"
      }
    }
  }
}
```

### Authentication
- Email/password authentication
- Role-based access control
- Secure user session management

## Future Enhancements

### Planned Features
- [ ] QR code scanning for battery identification
- [ ] Push notifications for status updates
- [ ] PDF receipt/bill generation
- [ ] Advanced search and filtering
- [ ] Data export functionality
- [ ] Multi-language support
- [ ] Dark theme support
- [ ] Backup and restore features

### Technical Improvements
- [ ] Unit and integration tests
- [ ] CI/CD pipeline setup
- [ ] Performance optimization
- [ ] Accessibility improvements
- [ ] Tablet layout optimization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review Firebase setup guides

---

**Note**: This Android app maintains feature parity with the original Flask web application while leveraging native Android capabilities and Firebase's real-time features for an enhanced mobile experience.