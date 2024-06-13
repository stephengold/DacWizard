/*
 Copyright (c) 2019-2023 Stephen Gold

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.wizard;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.AllowNull;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.FloatDialog;
import jme3utilities.ui.InputMode;

/**
 * Input mode for the "load" screen of DacWizard.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LoadMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final static Logger logger = Logger.getLogger(LoadMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized input mode.
     */
    LoadMode() {
        super("load");
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add default hotkey bindings. These bindings will be used if no custom
     * bindings are found.
     */
    @Override
    protected void defaultBindings() {
        bind(SimpleApplication.INPUT_MAPPING_EXIT, KeyInput.KEY_ESCAPE);
        bind(Action.editBindings, KeyInput.KEY_F1);
        bind(Action.editDisplaySettings, KeyInput.KEY_F2);

        bind(Action.previousScreen, KeyInput.KEY_PGUP, KeyInput.KEY_B);
        bind(Action.nextScreen, KeyInput.KEY_PGDN, KeyInput.KEY_N);

        bindSignal(CameraInput.FLYCAM_BACKWARD, KeyInput.KEY_S);
        bindSignal(CameraInput.FLYCAM_FORWARD, KeyInput.KEY_W);
        bindSignal(CameraInput.FLYCAM_LOWER, KeyInput.KEY_Z);
        bindSignal(CameraInput.FLYCAM_RISE, KeyInput.KEY_Q);
        bindSignal("orbitLeft", KeyInput.KEY_A);
        bindSignal("orbitRight", KeyInput.KEY_D);

        bind(Action.dumpPhysicsSpace, KeyInput.KEY_O);
        bind(Action.dumpRenderer, KeyInput.KEY_P);

        bind(SimpleApplication.INPUT_MAPPING_CAMERA_POS, KeyInput.KEY_C);
        bind(Action.toggleSkeleton, KeyInput.KEY_V);
    }

    /**
     * Initialize this (disabled) mode prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        // Configure the mouse cursor for this mode.
        AssetManager manager = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) manager.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }

    /**
     * Process an action from the keyboard or mouse.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        Validate.nonNull(actionString, "action string");
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Got action {0} ongoing={1}",
                    new Object[]{MyString.quote(actionString), ongoing});
        }

        boolean handled = false;
        if (ongoing) {
            Model model = DacWizard.getModel();
            handled = true;
            switch (actionString) {
                case Action.load:
                    model.load();
                    break;

                case Action.morePath:
                    model.morePath();
                    break;

                case Action.moreRoot:
                    model.moreRoot();
                    break;

                case Action.nextAnimation:
                    model.nextAnimation();
                    break;

                case Action.nextScreen:
                    nextScreen();
                    break;

                case Action.previousAnimation:
                    model.previousAnimation();
                    break;

                case Action.previousScreen:
                    model.unload();
                    previousScreen();
                    break;

                case Action.setAnimationTime:
                    setAnimationTime();
                    break;

                case Action.toggleSkeleton:
                    model.toggleShowingSkeleton();
                    break;

                default:
                    handled = false;
            }

            String prefix = Action.setAnimationTime + " ";
            if (!handled && actionString.startsWith(prefix)) {
                String argument = MyString.remainder(actionString, prefix);
                float time = Float.parseFloat(argument);
                model.setAnimationTime(time);
                handled = true;
            }
        }

        if (!handled) {
            getActionApplication().onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Advance to the "bones" screen if possible.
     */
    private void nextScreen() {
        String feedback = LoadScreen.feedback();
        if (feedback.isEmpty()) {
            setEnabled(false);
            InputMode bones = InputMode.findMode("bones");
            bones.setEnabled(true);
        }
    }

    /**
     * Go back to the "filePath" screen.
     */
    private void previousScreen() {
        setEnabled(false);
        InputMode filePath = InputMode.findMode("filePath");
        filePath.setEnabled(true);
    }

    /**
     * Process a "set animationTime" action: display a dialog to enter a new
     * animation time.
     */
    private static void setAnimationTime() {
        Model model = DacWizard.getModel();
        float duration = model.animationDuration();
        DialogController controller
                = new FloatDialog("Set", 0f, duration, AllowNull.No);

        float oldTime = model.animationTime();
        String defaultText = Float.toString(oldTime);

        LoadScreen screen = DacWizard.findAppState(LoadScreen.class);
        screen.closeAllPopups();
        screen.showTextEntryDialog("Enter the animation time (in seconds):",
                defaultText, Action.setAnimationTime + " ", controller);
    }
}
