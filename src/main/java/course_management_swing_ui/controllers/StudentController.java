package course_management_swing_ui.controllers;

import course_management_swing_ui.services.StudentService;
import course_management_swing_ui.util.EnumUtil;
import course_management_swing_ui.util.dto.*;
import course_management_swing_ui.models.Student;
import course_management_swing_ui.util.entitiesMappers.EnrollmentMapper;
import course_management_swing_ui.views.ViewManager;
import course_management_swing_ui.views.student.NewStudentView;
import course_management_swing_ui.views.student.ListStudentView;
import course_management_swing_ui.views.View;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static course_management_swing_ui.repositories.DbContext.enrollmentDbContext;
import static course_management_swing_ui.repositories.DbContext.studentDbContext;


//handles the user interaction as well as logic business of Student entities
public class StudentController extends BaseController {
    private final static StudentService studentService = new StudentService();
    public final static Vector<Vector<?>> dto = new Vector<>();

    public StudentController() {
        super();
    }

    public StudentController(View view) {
        super(view);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        view.disposeGUI();
    }

    /**
     * Handle events of NewStudentView, ListStudentView
     * @effects <pre>
     * All event's action:
     * Case(s) of NewStudentView
     *  Cancel:
     *      - close window
     *  Add:
     *      - add new student do DbContext
     *      - close window
     *
     * Case(s) of ListStudentView
     *  Add:
     *      - get the reference of NewStudentView (init if null)
     *      - then display it
     *  Check All / Uncheck All:
     *      - toggle check button in ListStudentView
     *      - check/uncheck all rows in ListStudentView.tblStudents
     *  Delete:
     *      - delete all selected items
     *      - fetch new data
     *      - notify ListStudentView that data changed
     *      - uncheck all selected items
     *  Update:
     *      - get the ref of student in DbContext
     *      - execute update by using studentRepository
     *      - fetch new data
     *      - notify ListStudentView that data changed
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
        if (view instanceof NewStudentView) {
            NewStudentView v = (NewStudentView) view;
            switch (command) {
                case "Cancel":
                    v.disposeGUI();
                    break;
                case "Add":
                    if (addStudent(v.getTxtName().getText(), v.getTxtDob().getText(), v.getTxtAddress().getText(), v.getTxtEmail().getText())) {
                        v.disposeGUI();
                    }
                    break;
                default:
                    break;
            }
        } else if (view instanceof ListStudentView) {
            ListStudentView v = (ListStudentView) view;
            JFrame gui = v.getGui();
            JTable tblStudents = v.getTblStudents();
            switch (command) {
                case "Add":
                    View addGUI = NewStudentView.getInstance();
                    if (addGUI == null) {
                        StudentController sc = new StudentController();
                        BaseController.controllers.add(sc);
                        try {
                            fetchData().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                        addGUI = NewStudentView.getInstance(view.getGui(), sc);
                        sc.setGui(addGUI);
                        ViewManager.viewMap.put(addGUI.hashCode(), addGUI);
                    }
                    addGUI.display();
                    break;
                case "  Check All  ":
                    setCheckAll(true);
                    break;
                case "Uncheck All":
                    setCheckAll(false);
                    break;
                case "Delete":
                    int result = JOptionPane.showConfirmDialog(gui, "Are you sure?", "Delete confirmation", JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        List<Student> students = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblStudents.getModel();
                        //DefaultTableModel associated with the JTable is used to iterate over each row and check if the corresponding checkbox (column 5) is selected.
                        for (int i = tblStudents.getRowCount() - 1; i >= 0; i--) {
                            boolean delete = (boolean) tm.getValueAt(i, 5);
                            if (delete) {
                                String id = String.valueOf(tm.getValueAt(i, 0));
                                students.add(studentService.findById(Integer.valueOf(id.substring(1))));
                            }
                        }
                        //If the number of selected students equals the total number of students in the database, 
//                       it indicates that all students are selected for deletion. In this case, the studentService.deleteAll() 
                        if (students.size() == studentDbContext.size()) {
                            studentService.deleteAll();
                        } else {
                            deleteStudent(students);
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
                    //      When you want to create a new Student object for updating,
                    //      which means neither resetStudentIdCount() nor fetchData() are called before.
                    //      Please use the CONSTRUCTOR or equivalent methods THAT NOT MODIFY the Student.idCount,
                    //      which play an important role the process of generating the unique Student.id.
                    //      If not, it will result in creating wrong id for the new Student Object, which will replace the existing one.
                    // Solution:
                    //      USE studentRepository.findById(id)
                    //      OR use stream API or for-loop for finding the needed object in Student Db Context,
                    //      then create new Student object with the Constructor annotated with @Safe
                    int editResult = JOptionPane.showConfirmDialog(gui, "Update all the selected rows?", "Update confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (editResult == JOptionPane.YES_OPTION) {
                        List<Student> students = new ArrayList<>();
                        DefaultTableModel tm = (DefaultTableModel) tblStudents.getModel();
                        for (int i = tblStudents.getRowCount() - 1; i >= 0; i--) {
                            boolean edit = (boolean) tm.getValueAt(i, 5);
                            if (edit) {
                                int id = Integer.parseInt(((String) tm.getValueAt(i, 0)).substring(1));
                                String name = (String) tm.getValueAt(i, 1);
                                Object dobO = tm.getValueAt(i, 2);
                                LocalDate dob = dobO instanceof String ? LocalDate.parse((String) dobO) : (LocalDate) dobO;
                                String address = (String) tm.getValueAt(i, 3);
                                String email = (String) tm.getValueAt(i, 4);
                                Student student = studentService.findById(id);
                                student.setName(name);
                                student.setDob(dob);
                                student.setAddress(address);
                                student.setEmail(email);
                                students.add(student);
                            }
                        }
                        updateStudent(students);
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
     * @modifies DbContext.studentDbContext, ViewManager.viewMap
     * @effects <pre>
     *     - add to the database the new Modulee which made of name, dob, address, email
     *     - fetch new data
     *     - notify the corresponding views in ViewManager.viewMap
     * </pre>
     */
    private boolean addStudent(String name, String dob, String address, String email) {
        try {
            studentService.add(new Student(name, LocalDate.parse(dob), address, email));
            fetchData().get();
            Optional<View> opt = ViewManager.viewMap.values().stream().filter(m -> m instanceof ListStudentView).findFirst();
            if (opt.isPresent()) {
                ListStudentView v = (ListStudentView) opt.get();
                v.notifyDataChanged();
            }
            return true;
        } catch (Exception e) {
            System.out.println("Invalid input for new Student!");
            JOptionPane.showMessageDialog(view.getGui(), "Invalid input for new Student!", "Failed To add new Student", JOptionPane.ERROR_MESSAGE);
            return false;
        }

    }

    /**
     * @effects execute deletion of all Student in students
     */
    private void deleteStudent(List<Student> students) {
        if (students != null && students.size() > 0) {
            studentService.delete(students);
        }
    }

    /**
     * @effects execute update of all Student in student
     */
    private void updateStudent(List<Student> students) {
        if (students != null && students.size() > 0) {
            studentService.update(students);
        }
    }

    /**
     * Get data from the DbContext. More specifically, get all Student and save it to studentDbContext.
     * @modifies DbContext.studentDbContext
     * @effects <pre>
     *      Clear old data from DbContext (Student only)
     *      Then process to add all the correspond Student(s) to DbContext.studentDbContext
     *      Update new data to this.dto
     * </pre>
     */
    public static CompletableFuture<Void> fetchData() {
        return CompletableFuture.runAsync(() -> {
            resetStudentIdCount();   // to clear the idCount stored in the Student class.
            studentDbContext.clear();  //Clears the studentDbContext (database context for students) and then populates 
            //it with all student records fetched from the database using studentService.findAll().
            studentDbContext.addAll(studentService.findAll());

            dto.clear();
            dto.addAll(DtoGenerator.getDto(studentDbContext));
            System.out.println("fetched new data from the database for: Student");

            EnrollmentMapper.getInstance().mapStudent();
            EnrollmentController.dtoIR.clear();
            EnrollmentController.dtoIR.addAll(DtoGenerator.getDto_initialReport(enrollmentDbContext));
            EnrollmentController.dtoAR.clear();
            EnrollmentController.dtoAR.addAll(DtoGenerator.getDto_assessmentReport(enrollmentDbContext));
            resetOtherControllerViews(EnumUtil.Controller.StudentController);
        });
    }

    public static CompletableFuture<Void> fetchData2() {
        return CompletableFuture.runAsync(() -> {
            resetStudentIdCount();
            studentDbContext.clear();
            studentDbContext.addAll(studentService.findAll());

            dto.clear();
            dto.addAll(DtoGenerator.getDto(studentDbContext));
            System.out.println("fetched new data from the database for: Student");
        });
    }

    /**
     * @effects delete all IdCount stored in the static HashMap called Student.idCount
     */
    private static void resetStudentIdCount() {
        Student.idCount.clear();
        studentDbContext.forEach(s -> Student.idCount.add(s.getNumericalId()));
    }

    /**
     * @effects set all rows in InitialReportView or AssessmentReportView with the declared value
     */
    private void setCheckAll(boolean value) {
        ListStudentView v = (ListStudentView) view;
        JTable tblStudents = v.getTblStudents();
        JButton btn = v.btnCheckAll;
        for (int i = 0; i < tblStudents.getRowCount(); i++) {
            tblStudents.setValueAt(value, i, 5);
        }
        if (value) {
            btn.setText("Uncheck All");
        } else {
            btn.setText("  Check All  ");
        }
    }
}
