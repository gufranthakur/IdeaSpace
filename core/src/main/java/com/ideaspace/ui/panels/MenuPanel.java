package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;

import java.awt.*;

public class MenuPanel extends Stage {

    private IdeaSpace ideaSpace;

    private Table root;
    private ISTable menuTable;
    private ISButton arduinoButton, dcMotorButton, escButton, fcButton, iphone17Button, joystickButton,
        l298Button, laptopFanButton, mechanicalKeyboardButton, ramButton, rpi_camButton, sd_cardButton,
        servoMotorButton;

    private final int menuButtonDimension = 200;

    public MenuPanel(IdeaSpace ideaSpace) {
        super(new ExtendViewport(Gdx.graphics.getWidth() - 450, Gdx.graphics.getWidth() - 450));
        this.ideaSpace = ideaSpace;

        root = new Table();
        root.setFillParent(true);
        root.top().center();

        createUI();
        addListener();
    }

    private void createUI() {

        menuTable = new ISTable("ui/png/menu/menu_bg.png");
        menuTable.top().left();
        menuTable.pad(30f);

        arduinoButton = new ISButton("ui/png/menu/arduino_uno.png");
        dcMotorButton = new ISButton("ui/png/menu/dc_motor.png");
        escButton = new ISButton("ui/png/menu/esp32.png");
        fcButton = new ISButton("ui/png/menu/fc.png");
        iphone17Button = new ISButton("ui/png/menu/iphone17.png");
        joystickButton = new ISButton("ui/png/menu/joystick_module.png");
        l298Button = new ISButton("ui/png/menu/l298_motor.png");
        laptopFanButton = new ISButton("ui/png/menu/laptop_fan.png");
        mechanicalKeyboardButton = new ISButton("ui/png/menu/mechanical_keyboard.png");
        ramButton = new ISButton("ui/png/menu/ram.png");
        sd_cardButton = new ISButton("ui/png/menu/sd_card_module.png");
        rpi_camButton = new ISButton("ui/png/menu/rpi_cam.png");

        menuTable.add(arduinoButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(dcMotorButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(escButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(fcButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);

        menuTable.add(iphone17Button).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.row();
        menuTable.add(joystickButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);


        menuTable.add(l298Button).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(laptopFanButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(mechanicalKeyboardButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(ramButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(rpi_camButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);

        menuTable.row();
        menuTable.add(sd_cardButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);
        menuTable.add(rpi_camButton).width(menuButtonDimension).height(menuButtonDimension).padRight(10f);


        root.add(menuTable).width(1100f).height(800f);

        this.addActor(root);
    }

    private void addListener() {
//        addModelToLibrary("Arduino-Uno", "models/microcontrollers/arduino_uno2222.glb");
//        addModelToLibrary("Esp32", "models/microcontrollers/esp32.glb");
//        addModelToLibrary("Iphone-17", "models/misc/iphone17pro.glb");
//        addModelToLibrary("Joystick-Module", "models/components/joystick_module.glb");
//        addModelToLibrary("Servo-Motor", "models/components/servomotor.glb");
//        addModelToLibrary("l298motordriver", "models/components/l298motordriver.glb");
//        addModelToLibrary("RaspberryPi", "models/microcontrollers/rpi4.glb");
//        addModelToLibrary("Rpi-cam", "models/components/rpicamera_split.glb");
//        addModelToLibrary("SD-Card Module", "models/components/sdcard.glb");
//        addModelToLibrary("DC Motor", "models/components/dcmotor.glb");
//        addModelToLibrary("DDR4", "models/components/ddr4.glb");
//        addModelToLibrary("Laptop Fan", "models/components/laptop_fan.glb");
//        addModelToLibrary("Mechanical-Keyboard", "models/misc/mechanicalkeyboard_split.glb");

        arduinoButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
               ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Arduino-Uno"));
               ideaSpace.toggleMenuFlag();
            }
        });

        dcMotorButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("DC Motor"));
                ideaSpace.toggleMenuFlag();
            }
        });

        escButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Arduino-Uno"));
                ideaSpace.toggleMenuFlag();
            }
        });

        fcButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Arduino-Uno"));
                ideaSpace.toggleMenuFlag();
            }
        });

        iphone17Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Iphone-17"));
                ideaSpace.toggleMenuFlag();
            }
        });

        joystickButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Joystick-Module"));
                ideaSpace.toggleMenuFlag();
            }
        });

        l298Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("l298motordriver"));
                ideaSpace.toggleMenuFlag();
            }
        });

        laptopFanButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Laptop Fan"));
                ideaSpace.toggleMenuFlag();
            }
        });

        mechanicalKeyboardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Mechanical-Keyboard"));
                ideaSpace.toggleMenuFlag();
            }
        });

        ramButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("DDR4"));
                ideaSpace.toggleMenuFlag();
            }
        });

        rpi_camButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("Rpi-cam"));
                ideaSpace.toggleMenuFlag();
            }
        });

        sd_cardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!ideaSpace.menuFlag) return;
                ideaSpace.modelHandler.loadModel(ideaSpace.modelHandler.modelLibrary.get("SD-Card Module"));
                ideaSpace.toggleMenuFlag();
            }
        });

//        servoMotorButton.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//
//            }
//        });





    }

    public void render() {
        this.act();
        this.draw();
    }

    public void resize(int width, int height) {
        this.getViewport().update(width, height, false);
    }



}
