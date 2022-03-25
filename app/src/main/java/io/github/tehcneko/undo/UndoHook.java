package io.github.tehcneko.undo;

import android.view.Menu;
import android.view.MenuItem;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UndoHook implements IXposedHookLoadPackage {
    private static final String TAG = "UndoHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            var RClass = XposedHelpers.findClass("com.android.internal.R$string", loadPackageParam.classLoader);
            var undoId = XposedHelpers.getStaticIntField(RClass, "undo");
            var redoId = XposedHelpers.getStaticIntField(RClass, "redo");
            var textActionModeCallbackClass = XposedHelpers.findClass("android.widget.Editor$TextActionModeCallback", loadPackageParam.classLoader);
            var editorClass = XposedHelpers.findClass("android.widget.Editor", loadPackageParam.classLoader);
            var editorField = XposedHelpers.findField(textActionModeCallbackClass, "this$0");
            var textViewField = XposedHelpers.findField(editorClass, "mTextView");
            XposedHelpers.findAndHookMethod(
                    textActionModeCallbackClass,
                    "populateMenuWithItems",
                    Menu.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            var editor = editorField.get(param.thisObject);
                            var textView = textViewField.get(editor);
                            var menu = (Menu) param.args[0];
                            var undoItem = menu.findItem(undoId);
                            if (undoItem == null) {
                                if ((boolean) XposedHelpers.callMethod(textView, "canUndo")) {
                                    menu.add(Menu.NONE, android.R.id.undo, 2, undoId)
                                            .setAlphabeticShortcut('z')
                                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                                }
                            } else {
                                undoItem.setVisible((boolean) XposedHelpers.callMethod(textView, "canUndo"));
                            }
                            var redoItem = menu.findItem(redoId);
                            if (redoItem == null) {
                                if ((boolean) XposedHelpers.callMethod(textView, "canRedo")) {
                                    menu.add(Menu.NONE, android.R.id.redo, 3, redoId)
                                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                                }
                            } else {
                                redoItem.setVisible((boolean) XposedHelpers.callMethod(textView, "canRedo"));
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
