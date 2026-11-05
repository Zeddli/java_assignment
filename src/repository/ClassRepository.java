package repository;

import model.ClassSession;
 
public class ClassRepository extends BaseRepository<ClassSession> {
    public ClassRepository() { super("classes.txt"); }
 
    @Override
    protected ClassSession fromFields(String[] f) { return ClassSession.fromFields(f); }
}

