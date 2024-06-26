package course_management_swing_ui.controllers;

import course_management_swing_ui.models.Enrollment;
import course_management_swing_ui.models.Module;
import course_management_swing_ui.models.Student;
import course_management_swing_ui.services.EnrollmentService;
import course_management_swing_ui.services.ModuleService;
import course_management_swing_ui.services.StudentService;
import course_management_swing_ui.util.EnumUtil;
import course_management_swing_ui.util.dto.DtoGenerator;
import course_management_swing_ui.views.View;
import course_management_swing_ui.views.ViewManager;
import course_management_swing_ui.views.enrollment.AssessmentReportView;
import course_management_swing_ui.views.enrollment.NewEnrollmentView;
import course_management_swing_ui.views.enrollment.InitialReportView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static course_management_swing_ui.repositories.DbContext.enrollmentDbContext;

//handles the user interaction as well as logic business of Enrollment entities
public class EnrollmentController extends BaseController {
    private final static StudentService studentService = new StudentService();
    private final static ModuleService moduleService = new ModuleService();
    private final static EnrollmentService enrollmentService = new EnrollmentService();
    public final static Vector<Vector<?>> dtoIR = new Vector<>();
    public final static Vector<Vector<?>> dtoAR = new Vector<>();

    public EnrollmentController() {
        super();
    }

    public EnrollmentController(View view) {
        super(view);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        view.disposeGUI();
    }

    /**
     * Handle events of NewEnrollmentView, InitialReportView, AssessmentReportView
     * @effects <pre>
     * All event's action:
     * Case(s) of NewEnrollmentView
     *  Cancel:
     *      - close window
     *  Add:
     *      - find the student, module in DbContext
     *      - add new Enrollment to the DbContext
     *      - close window
     *
     * Case(s) of InitialReportView
     *  Add:
     *      - get the reference of NewEnrollmentView (init if null)
     *      - then display it
     *  Check All / Uncheck All:
     *      - toggle check button in InitialReportView / AssessmentReportView
     *      - check/uncheck all rows in InitialReportView.tblEnrollment / AssessmentReportView.tblEnrollment
     *  Delete:
     *      - delete all selected items
     *      - fetch new data
     *      - notify InitialReportView that data changed
     *      - uncheck all selected items
     *  Update:
     *      - get the ref of student & module in DbContext
     *      - get the ref of enrollment in DbContext
     *      - execute update by using enrollmentRepository
     *      - fetch new data
     *      - notify InitialReportView that data changed
     *      - uncheck all selected items
     *  Refresh Data
     *      - fetch new data
     *  Close
     *      - dispose views
     *  </pre>
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (view instanceof NewEnrollmentView) {
            NewEnrollmentView v = (NewEnrollmentView) view;
            switch (command) {
                case "Cancel":
                    v.disposeGUI();
                    break;
                case "Add":
                    Student student = studentService.findById(Integer.valueOf(v.getSid().substring(1)));
                    Module module = moduleService.findById(v.getmCode());
                    addEnrollment(enrollmentDbContext.size() + 1, student, module, Double.parseDouble(v.getTxtIm().getText()), Double.parseDouble(v.getTxtEm().getText()));
                    v.disposeGUI();
                    break;
                default:
                    break;
            }
        } else if (view instanceof InitialReportView) {
            InitialReportView v = (InitialReportView) view;
            JFrame gui = v.getGui();
            JTable tblEnrollments = v.getTblEnrollment();
            switch (command) {
                case "Add":
                    View addGUI = NewEnrollmentView.getInstance();
                    if (addGUI == null) {
                        EnrollmentController ec = new EnrollmentController();
                        BaseController.controllers.add(ec);
                        try {
                            EnrollmentController.fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        addGUI = NewEnrollmentView.getInstance(view.getGui(), ec);
                        ec.setGui(addGUI);
                        ViewManager.viewMap.put(addGUI.hashCode(), addGUI);
                    }
                    addGUI.display();
                    break;
                case "  Check All  ":
                    setCheckAll(true);
                    //JButton btn = (JButton) e.getSource();
                    break;
                case "Uncheck All":
                    setCheckAll(false);
                    break;
                case "Delete":
                    int result = JOptionPane.showConfirmDialog(gui, "Are you sure?", "Delete confirmation", JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        List<Enrollment> enrollments = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblEnrollments.getModel();
                        for (int i = tblEnrollments.getRowCount() - 1; i >= 0; i--) {
                            boolean delete = (boolean) tm.getValueAt(i, 5);
                            if (delete) {
                                int id = (int) tm.getValueAt(i, 0);
                                enrollments.add(enrollmentService.findById(id));
                            }
                        }
                        if (enrollments.size() == enrollmentDbContext.size()) {
                            enrollmentService.deleteAll();
                        } else {
                            deleteEnrollment(enrollments);
                        }

                        try {
                            fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        // reset views in order to remove row(s)
                        v.notifyDataChanged();
                        setCheckAll(false);
                    }
                    break;
                case "Update":
                    // REMINDER:
                    //      When you want to create a new Enrollment object for updating,
                    //      which means neither resetEnrollmentIdCount() nor fetchData() are called before.
                    //      Please use the CONSTRUCTOR or equivalent methods THAT NOT MODIFY Db Context
                    //      which play an important role the process of generating the unique Enrollment.code.
                    //      If not, it will result in creating wrong id for the new Enrollment Object, which will replace the existing one.
                    // Solution:
                    //      use stream API or for-loop for finding the needed object in Enrollment Db Context,
                    //      then create new Enrollment object with the Constructor annotated with @Safe
                    int editResult = JOptionPane.showConfirmDialog(gui, "Update all the selected rows?", "Update confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (editResult == JOptionPane.YES_OPTION) {
                        List<Enrollment> enrollments = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblEnrollments.getModel();
                        for (int i = tblEnrollments.getRowCount() - 1; i >= 0; i--) {
                            boolean edit = (boolean) tm.getValueAt(i, 5);
                            if (edit) {
                                int id = (int) tm.getValueAt(i, 0);
                                int sid = Integer.parseInt(((String) tm.getValueAt(i, 1)).substring(1));
                                String sName = (String) tm.getValueAt(i, 2);
                                String mCode = (String) tm.getValueAt(i, 3);
                                String mName = (String) tm.getValueAt(i, 4);
                                Enrollment enrollment = enrollmentService.findById(id);
                                double im = enrollment.getInternalMark(), em = enrollment.getExaminationMark();
                                Student student = studentService.findById(sid);
                                student.setName(sName);
                                studentService.update(student);
                                Module module = moduleService.findById(mCode);
                                module.setName(mName);
                                moduleService.update(module);
                                enrollment.setStudent(student);
                                enrollment.setModule(module);
                                enrollment.setInternalMark(im);
                                enrollment.setExaminationMark(em);
                                enrollments.add(enrollment);
                            }
                        }
                        updateEnrollment(enrollments);
                        try {
                            fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        v.notifyDataChanged();
                        setCheckAll(false);
                    }
                    break;
                case "Refresh Data":
                    try {
                        fetchData().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
                    v.notifyDataChanged();
                    setCheckAll(false);
                    break;

                case "Close":
                    view.shutDown();
                    break;
                default:
                    break;
            }
        } else if (view instanceof AssessmentReportView) {
            AssessmentReportView v = (AssessmentReportView) view;
            JFrame gui = v.getGui();
            JTable tblEnrollments = v.getTblEnrollment();
            switch (command) {
                case "Add":
                    View addGUI = NewEnrollmentView.getInstance();
                    if (addGUI == null) {
                        EnrollmentController sc = new EnrollmentController();
                        BaseController.controllers.add(sc);
                        try {
                            EnrollmentController.fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        addGUI = NewEnrollmentView.getInstance(view.getGui(), sc);
                        sc.setGui(addGUI);
                        ViewManager.viewMap.put(addGUI.hashCode(), addGUI);
                    }
                    addGUI.display();
                    break;
                case "  Check All  ":
                    setCheckAll(true);
                    //JButton btn = (JButton) e.getSource();
                    break;
                case "Uncheck All":
                    setCheckAll(false);
                    break;
                case "Delete":
                    int result = JOptionPane.showConfirmDialog(gui, "Are you sure?", "Delete confirmation", JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        List<Enrollment> enrollments = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblEnrollments.getModel();
                        for (int i = tblEnrollments.getRowCount() - 1; i >= 0; i--) {
                            boolean delete = (boolean) tm.getValueAt(i, 6);
                            if (delete) {
                                int id = (int) tm.getValueAt(i, 0);
                                enrollments.add(enrollmentService.findById(id));
                            }
                        }
                        if (enrollments.size() == enrollmentDbContext.size()) {
                            enrollmentService.deleteAll();
                        } else {
                            deleteEnrollment(enrollments);
                        }

                        try {
                            fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        // reset views in order to remove row(s)
                        v.notifyDataChanged();
                        setCheckAll(false);
                    }
                    break;
                case "Update":
                    int editResult = JOptionPane.showConfirmDialog(gui, "Update all the selected rows?", "Update confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (editResult == JOptionPane.YES_OPTION) {
                        List<Enrollment> enrollments = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblEnrollments.getModel();
                        for (int i = tblEnrollments.getRowCount() - 1; i >= 0; i--) {
                            boolean edit = (boolean) tm.getValueAt(i, 6);
                            if (edit) {
                                int id = (int) tm.getValueAt(i, 0);
                                int sid = Integer.parseInt(((String) tm.getValueAt(i, 1)).substring(1));
                                String mCode = (String) tm.getValueAt(i, 2);
                                Object imO = tm.getValueAt(i, 3), emO = tm.getValueAt(i, 4);
                                double im = imO instanceof String ? Double.parseDouble((String) imO) : (double) imO;
                                double em = emO instanceof String ? Double.parseDouble((String) emO) : (double) emO;
                                Enrollment enrollment = enrollmentService.findById(id);
                                Student student = studentService.findById(sid);
                                Module module = moduleService.findById(mCode);
                                enrollment.setStudent(student);
                                enrollment.setModule(module);
                                enrollment.setInternalMark(im);
                                enrollment.setExaminationMark(em);
                                enrollments.add(enrollment);
                            }
                        }
                        updateEnrollment(enrollments);
                        try {
                            fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        v.notifyDataChanged();
                        setCheckAll(false);
                    }
                    break;
                case "Refresh Data":
                    try {
                        fetchData().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
                    v.notifyDataChanged();
                    setCheckAll(false);
                    break;

                case "Close":
                    view.shutDown();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @modifies DbContext.enrollmentDbContext, ViewManager.viewMap
     * @effects <pre>
     *     - add to the database the new enrollment which made of id, student, module, im , em
     *     - fetch new data
     *     - notify the corresponding views in ViewManager.viewMap
     * </pre>
     */
    private void addEnrollment(int id, Student student, Module module, double im, double em) {
        if (Enrollment.validateId(id) && Enrollment.validateStudent(student) && Enrollment.validateModule(module) && Enrollment.validateMark(im) && Enrollment.validateMark(em)) {
            try {
                enrollmentService.add(new Enrollment(id, student, module, im, em));
                fetchData().get();
                Optional<View> opt1 = ViewManager.viewMap.values().stream().filter(m -> m instanceof InitialReportView).findFirst();
                Optional<View> opt2 = ViewManager.viewMap.values().stream().filter(m -> m instanceof AssessmentReportView).findFirst();
                if (opt1.isPresent()) {
                    InitialReportView v = (InitialReportView) opt1.get();
                    v.notifyDataChanged();
                }
                if (opt2.isPresent()) {
                    AssessmentReportView v = (AssessmentReportView) opt2.get();
                    v.notifyDataChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @effects execute deletion of all Enrollment in enrollments
     */
    private void deleteEnrollment(List<Enrollment> enrollments) {
        if (enrollments != null && enrollments.size() > 0) {
            enrollmentService.delete(enrollments);
        }
    }

    /**
     * @effects execute update of all Enrollment in enrollments
     */
    private void updateEnrollment(List<Enrollment> enrollments) {
        if (enrollments != null && enrollments.size() > 0) {
            enrollmentService.update(enrollments);
        }
    }

    /**
     * Get data from the DbContext. More specifically, get all enrollments and save it to enrollmentDbContext.
     * @modifies DbContext
     * @effects <pre>
     *      Clear old data from DbContext (Student, Module, Enrollment)
     *      StudentController.fetchData(); -> get all students
     *      ModuleController.fetchData();  -> get all module
     *      Then process to add all the correspond Enrollment(s) to DbContext.enrollmentRepository
     *      Update new data to Dto(s)
     * </pre>
     */
    public static CompletableFuture<Void> fetchData() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("----------------------------");
            enrollmentDbContext.clear();

            CompletableFuture<Void> studentTask = StudentController.fetchData2();
            CompletableFuture<Void> moduleTask = ModuleController.fetchData2();
            try {
                CompletableFuture.allOf(studentTask, moduleTask).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            enrollmentDbContext.addAll(enrollmentService.findAll());

            dtoIR.clear();
            dtoIR.addAll(DtoGenerator.getDto_initialReport(enrollmentDbContext));

            dtoAR.clear();
            dtoAR.addAll(DtoGenerator.getDto_assessmentReport(enrollmentDbContext));
            System.out.println("fetched new data from the database for: Enrollment");
            System.out.println("----------------------------");

            resetOtherControllerViews(EnumUtil.Controller.EnrollmentController);
        });
    }

    /**
     * @effects set all rows in InitialReportView or AssessmentReportView with the declared value
     */
    private void setCheckAll(boolean value) {
        if (view instanceof InitialReportView) {
            InitialReportView v = (InitialReportView) view;
            JTable tblEnrollments = v.getTblEnrollment();
            JButton btn = v.btnCheckAll;
            for (int i = 0; i < tblEnrollments.getRowCount(); i++) {
                tblEnrollments.setValueAt(value, i, 5);
            }
            if (value) {
                btn.setText("Uncheck All");
            } else {
                btn.setText("  Check All  ");
            }
        } else {
            AssessmentReportView v = (AssessmentReportView) view;
            JTable tblEnrollments = v.getTblEnrollment();
            JButton btn = v.btnCheckAll;
            for (int i = 0; i < tblEnrollments.getRowCount(); i++) {
                tblEnrollments.setValueAt(value, i, 6);
            }
            if (value) {
                btn.setText("Uncheck All");
            } else {
                btn.setText("  Check All  ");
            }
        }
    }
}
