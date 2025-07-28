# Driver Drowsiness Detection App 🚗 

An Android based Final Year Project built in Java using Android Studio. This app helps reduce road accidents by detecting driver drowsiness in real time, providing alert mechanisms, and offering emergency support features even without internet access.

## Key Features 📱

-  Real time eye detection using front camera 👁️
-  Instant alerts (sound/vibration) on drowsiness 🔊
-  AI powered music player 🎵 
-  One tap SOS system for emergencies 🆘
-  Live location sharing for immediate help 📍
-  User login and authentication 🔐 
-  Logs and displays drowsiness alert history 📈 
-  Offline functionality available 📶

## Tech Stack 🛠️ 

- **Java**
- **Android Studio**
- **XML (UI Design)**
- **ML Kit** (for face and eye detection)
- **CameraX** (for accessing front camera)
- **Room Database** (for local data storage)
- **Firebase Authentication** (for login/signup)
- **Google Maps API** (for location)
- **MediaPlayer** (for music)
- **MVVM Architecture Pattern**

## App Modules Overview 📂 

| Module            | Description                                                                 |
|-------------------|-----------------------------------------------------------------------------|
| Authentication | User login/signup using Firebase                                            |
| Eye Monitoring   | Detects drowsiness using real time eye tracking via ML Kit                 |
| Alert System    | Plays sound or vibrates to alert drowsy driver                             |
| Music Detection | AI module to detect music type and recommend energy boosting tracks         |
| SOS Module      | One tap emergency alert with real time location sharing                     |
| Location Module | Captures and shares driver’s current GPS location during alert              |
| Log Module      | Stores alert and activity history locally using Room                        |

## How the Drowsiness Detection Works 🧠 

- Uses the **front camera** to monitor eye movements
- Calculates **Eye Aspect Ratio (EAR)** to detect eye closure duration
- Triggers alerts if eyes remain closed beyond a threshold

## Screenshots 📸 

![Picture8](https://github.com/user-attachments/assets/d433b939-7024-4caa-a6ad-96eaf8b424c3)
![Picture7](https://github.com/user-attachments/assets/43b73285-83ed-4f2b-b9e5-5a4dd8cf8bb6)
![Picture5](https://github.com/user-attachments/assets/6e9b66d7-2c91-47d5-b5dd-eaa25ced4378)
![Picture2](https://github.com/user-attachments/assets/4d1e79c4-bfda-46a3-9937-f7171ef10446)






##  Getting Started 🚀

1. Clone or download this repository.
2. Open in **Android Studio**.
3. Connect your device or emulator.
4. Add your `google-services.json` (for Firebase).
5. Run the project and grant camera & location permissions.

>  Permissions Required ⚠️
> - Camera
> - Location
> - Internet (for Firebase)
> - Audio (for music detection)

##  Academic Info 🎓

> This project was developed as part of the **Bachelor of Science in Computer Science (BSCS)** Final Year Project at **COMSATS University, Sahiwal Campus**.

## Developer 👨‍💻

**Muhammad Hamza Salman**  
Android Developer | AI Enthusiast  
• [LinkedIn]([https://www.linkedin.com/](https://www.linkedin.com/in/codew-hmzii/))

## License 📃 

This project is for academic and educational purposes. You may modify or use it non commercially with credit.
