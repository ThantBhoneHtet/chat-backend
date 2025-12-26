package tbh.chat_app.utils;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.Firestore;

import tbh.chat_app.model.User;

@Repository
public class UserCustomRepo {
    private final Firestore firestore;

    private final String COLLECTION_NAME = "users";


    public UserCustomRepo(Firestore firestore) {
        this.firestore = firestore;
    }

    public boolean existsByEmail(String email) {
        boolean flag = false;
        try {
            flag = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email).whereEqualTo("email", email).get().get().isEmpty();
        } catch (Exception e) {
            return false;
        }
        return !(flag);
    }

    // public DocumentReference getDocumentReference(String id) {
    //     return firestore.collection("users").document(id);
    // }

    public List<User> searchUsersByKeyword(String nameKeyword) {
        try {
            return firestore.collection(COLLECTION_NAME)
                    .whereGreaterThanOrEqualTo("firstName", nameKeyword)
                    .whereLessThanOrEqualTo("firstName", nameKeyword + '\uf8ff')
                    .get()
                    .get()
                    .toObjects(User.class);
        } catch (Exception e) {
            return List.of();
        }
    }

}
