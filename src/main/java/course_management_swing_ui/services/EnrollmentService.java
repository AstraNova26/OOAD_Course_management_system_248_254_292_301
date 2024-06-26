package course_management_swing_ui.services;

import course_management_swing_ui.models.Enrollment;
import course_management_swing_ui.repositories.EnrollmentRepository;
import course_management_swing_ui.repositories.db.DbConnect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Overview Implementation of Service for Enrollment.
 */
public class EnrollmentService implements Service<Enrollment, Integer> {
    private final EnrollmentRepository enrollmentRepository = new EnrollmentRepository();

    /**
     * add Object to the Database
     * @param obj
     * @requires obj != null
     */
    @Override
    public void add(Enrollment obj) {
        try (Connection conn = DbConnect.getConnection()) {
            enrollmentRepository.add(obj, conn).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * add all Objects in objs to the Database
     * @param objs
     * @requires objs != null
     */
    @Override
    public void addAll(Collection<Enrollment> objs) {
        try (Connection conn = DbConnect.getConnection()) {
            try {
                conn.setAutoCommit(false);
                enrollmentRepository.addAll(objs, conn).get();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * read and return T
     * @param id
     * @requires id != null
     */
    @Override
    public Enrollment findById(Integer id) {
        try (Connection conn = DbConnect.getConnection()) {
            return enrollmentRepository.findById(id, conn).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * return a list of all objects of T
     * @param ids
     * @requires ids != null
     */
    @Override
    public List<Enrollment> findById(Collection<Integer> ids) {
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection conn = DbConnect.getConnection()) {
            enrollments.addAll(enrollmentRepository.findById(ids, conn).get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return enrollments;
    }

    /**
     * return a list of all objects of T
     * @requires ids != null
     */
    @Override
    public List<Enrollment> findAll() {
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection conn = DbConnect.getConnection()) {
            enrollments.addAll(enrollmentRepository.findAll(conn).get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return enrollments;
    }

    /**
     * update the row that share the primary key with obj
     * @param obj
     * @requires obj != null
     */
    @Override
    public void update(Enrollment obj) {
        try (Connection conn = DbConnect.getConnection()) {
            enrollmentRepository.update(obj, conn).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * update all rows that share the primary key with each obj in objs
     * @param objs
     * @requires obj != null
     */
    @Override
    public void update(Collection<Enrollment> objs) {
        try (Connection conn = DbConnect.getConnection()) {
            try {
                conn.setAutoCommit(false);
                enrollmentRepository.update(objs, conn).get();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * delete the row that share the primary key with obj
     * @param obj
     * @requires obj != null
     */
    @Override
    public void delete(Enrollment obj) {
        try (Connection conn = DbConnect.getConnection()) {
            enrollmentRepository.delete(obj, conn).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * delete every row that share the primary key with each obj in objs
     * @param objs
     * @requires obj != null
     */
    @Override
    public void delete(Collection<Enrollment> objs) {
        try (Connection conn = DbConnect.getConnection()) {
            try {
                conn.setAutoCommit(false);
                enrollmentRepository.delete(objs, conn).get();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * delete all record(s) in the table of database
     * @requires conn != null
     */
    @Override
    public void deleteAll() {
        try (Connection conn = DbConnect.getConnection()) {
            enrollmentRepository.deleteAll(conn).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
