package com.org.jzprinter.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.dao.MaterialDao;
import com.org.jzprinter.database.dao.PrintProgressDao;
import com.org.jzprinter.database.dao.PrintTaskDao;
import com.org.jzprinter.database.dao.StudentDao;
import com.org.jzprinter.database.entity.MaterialEntity;
import com.org.jzprinter.database.entity.PrintProgressEntity;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.database.entity.StudentEntity;
import com.org.jzprinter.repository.MaterialRepository;
import com.org.jzprinter.repository.PrintProgressRepository;
import com.org.jzprinter.repository.PrintTaskRepository;
import com.org.jzprinter.repository.StudentRepository;

@Database(entities = {
    StudentEntity.class,
    MaterialEntity.class,
    PrintTaskEntity.class,
    PrintProgressEntity.class
}, version = 4, exportSchema = true)
@TypeConverters({IntegerListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE student ADD COLUMN businessId TEXT");
            database.execSQL("ALTER TABLE print_task ADD COLUMN targetName TEXT NOT NULL DEFAULT ''");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE print_task ADD COLUMN businessId TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE print_task ADD COLUMN editionName TEXT NOT NULL DEFAULT ''");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "jz_print_db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract StudentDao studentDao();
    public abstract MaterialDao materialDao();
    public abstract PrintTaskDao printTaskDao();
    public abstract PrintProgressDao printProgressDao();

    public PrintTaskRepository printTaskRepository() {
        return new PrintTaskRepository(printTaskDao());
    }

    public StudentRepository studentRepository() {
        return new StudentRepository(studentDao());
    }

    public PrintProgressRepository printProgressRepository() {
        return new PrintProgressRepository(printProgressDao());
    }

    public MaterialRepository materialRepository() {
        return new MaterialRepository(materialDao());
    }
}
