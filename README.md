# IdeaSpace 

A prototype 3D learning platform where users can view 3D models, break them down and control them in the virtual space using hand gestures. A project that was made for learning purposes.
The project can be used as a learning / demonstration tool, showcasing inner workins of a topic or breakdowns. 

It uses the desktop camera to capture video feed using a python process (being executed by Java). The python process uses websockets to send gestures to the Java libGDX applciation, and it responds accordingly. 

The project is mostly incomplete (and abandoned) but could be continued later. If anything, I hope the existing source code of the Virtual hand implementation could be really helpful. If there's enough demand I will create a seperate repo/tutorial just for it :D 

## Technical Specifications

- Programming Langugage : Java 21 + Python 3.11
- Frameworks : LibGDX, OpenCV and Mediapipe
- Developed and tested on : Windows 11, MacOS 26.4, Ubuntu 24

## Resources Used 
The 2D assets used in the application were all designed by us. However most of the 3D models were downloaded from sketchfab. The credits and attributions will be added shortly.

*(Note : because of LibGDX's notorious scene2D and skin system, I had to design every single UI component and used them as a png. Forgive me for that :)  )*

## To run the project
(IntelliJ IDEA) 

- clone the repository
- Have python installed
- Create a venv (Virtual environment) in ``` /python ``` directory
- install mediapipe, and opencv (There are currently on-going issues with the versions)

- Within IntelliJ, have gradle setup the project
- Once setup, run the desktoplauncher setup (LWJGLLauncher) 

## Screenshots 

<img width="1469" height="843" alt="Screenshot 2026-05-13 at 3 14 20 PM" src="https://github.com/user-attachments/assets/79078e94-c417-41e4-84cc-4f588dfa6d1c" />
<img width="1463" height="842" alt="Screenshot 2026-05-13 at 3 13 47 PM" src="https://github.com/user-attachments/assets/b2a8f903-1b0a-42f8-acd2-8e02e67ee630" />

## Contributors 

This was a group project, a special thank you to my group members *Hasan Ghawte* , *Ayesha Ansari* and *Iqra Choudhary* for developing the project



