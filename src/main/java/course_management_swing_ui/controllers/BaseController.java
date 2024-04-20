package course_management_swing_ui.controllers;

import course_management_swing_ui.util.EnumUtil;
import course_management_swing_ui.views.View;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.*;
import java.util.stream.Collectors;

//which handles the user interaction of its views
public abstract class BaseController extends WindowAdapter implements ActionListener {
    protected View view;
    public final static Set<BaseController> controllers = new HashSet<>();
    //constructor overloading
    public BaseController() {

    }

    public BaseController(View gui) {
        setGui(gui);
    }

    /**
     * @effects sets <tt>this.gui = gui</tt>
     */
    public void setGui(View gui) {
        this.view = gui;
    }

    /**
     * @effects return views
     */
    public View getView() {
        return view;
    }

//    resetOtherControllerViews(EnumUtil.Controller cause) method:
//
//    	This is a static method used to reset views of other controllers based on the specified cause.
//    	It takes an EnumUtil.Controller parameter (cause) to determine which controllers' views need to be reset.
//    	It iterates over the controllers set, which holds references to all active controllers.
//    	Based on the cause, it filters the controllers and extracts their associated views (View instances).
//    	It then calls the notifyDataChanged() method on each extracted view to notify them that their data has changed, triggering them to update their display.
//    	The method uses Java streams and lambda expressions to filter controllers and map them to their associated views efficiently.
    public static void resetOtherControllerViews(EnumUtil.Controller cause) {
        List<View> modifies;   //list will hold the views that need to be modified/reset.
        switch (cause) {
            case StudentController:
            case ModuleController:
                modifies = controllers.stream()
                        .filter(c -> {
                            if (c == null) return false;
                            String name = c.getClass().getSimpleName();
                            return Objects.equals(name, EnumUtil.Controller.EnrollmentController.toString());
                        })
                        .map(BaseController::getView)
                        .collect(Collectors.toList());
                break;
            case EnrollmentController:
                modifies = controllers.stream()
                        .filter(c -> {
                            if (c == null) return false;
                            String name = c.getClass().getSimpleName();
                            return Objects.equals(name, EnumUtil.Controller.StudentController.toString()) || Objects.equals(name, EnumUtil.Controller.ModuleController.toString());
                        })
                        .map(BaseController::getView)
                        .collect(Collectors.toList());
                break;
            default:
                return;
        }
        modifies.forEach(v -> {
            if (v == null) return;
            v.notifyDataChanged();
        });
    }
}
