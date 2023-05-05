# TremorSoft
An decision support application for differential diagnosis between Parkinson's Disease and Essential Tremor

Copyright (c) 2022. TremorSoft
All Rights Reserved. This work is protected by copyright laws and international treaties.

![TremorSoft Logo](https://github.com/jdloaizad/TremorSoft/blob/master/app/src/main/res/drawable/logo.png)

## Purpose

TremorSoft is a mobile application designed as a diagnostic support tool for patients with movement disorders, specifically those with suspected Parkinson's Disease (PD) or Essential Tremor (ET). With this app, in a few minutes and through a few simple steps, physicians and movement disorder specialists can record and classify their patients' hand tremors during clinical routine by using the internal accelerometer and gyroscope of a smartphone device or an Xsens Dot sensor connected to the app wirelessly. The recorded data can also be used for monitoring disease progression and assessing treatment effectiveness.


## Installation

To install the application, follow the steps below:

1. Download the application source code from the GitHub repository.
2. Import the project into IntelliJ IDEA or Android Studio.
3. Compile the project and generate the APK file of the application.
4. Install the application on your mobile device through the "install from APK" option in your device settings.

IMPORTANT: To use the Xsens Dot sensor, it is necessary to purchase and connect the sensor to the mobile device following the instructions provided by the application.

## Use

Before recording the tremor signals, the user must provide the patient's demographic data, such as age, gender, family history and health status. Once this step is completed, the user can select from where to record the tremor signals ( Internal smartphone sensors or from the Xsens Dot).

Then, the user must record the tremor signals in two different positions for 15 seconds each: rest and posture. For the resting position, the user must rest his or her arms on the armrest of a chair without tensing or contracting the hand while the app records the tremor signals. For the posture position, the user should keep the arms extended and parallel to the floor while the application records the tremor signals. It is important to clarify that the application will show an example image for each position to ensure that the recording in both positions is done correctly.

Once the tremor signals have been recorded in both positions, the user can repeat the recording if deemed necessary or send the data to the webserver to be processed and classified through machine learning models. Once the hand tremor is classified, the web server will return the result which will be displayed in the mobile application. Also, suppose the classification of the recorded data matches the diagnosis confirmed by a movement disorder specialist. In that case, it can be stored in a database to reinforce the machine learning models.

## Additional notes
- The application is not a substitute for professional medical diagnosis and treatment.
- It is recommended that users share the recorded data with their physician or movement disorder specialist for further evaluation and analysis.
- If you experience any problems with the application, please get in touch with us via email: julian.david.loaiza@upc.edu.
