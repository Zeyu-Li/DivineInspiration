package com.DivineInspiration.experimenter.Controller;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import androidx.annotation.NonNull;

import com.DivineInspiration.experimenter.Model.IdGen;
import com.DivineInspiration.experimenter.Model.User;

import com.DivineInspiration.experimenter.Model.UserContactInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class talks to the FireStore database
 * in order to store and retrieve user data.
 * It also manages the storage of user data locally on the device.
 * The class uses singleton pattern.
 */
public class UserManager {
    /* Reference citation:
    Name: Obaro Ogbo
    Link: https://www.androidauthority.com/how-to-store-data-locally-in-android-app-717190/
    Date: September 21, 2016
    License: unknown
    Usage: android local storage
     */
    private SharedPreferences pref;                 // Used to get the data about the local user which is stored locally on the device.
    private static UserManager singleton = null;    // Singleton object

    private User user;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final String TAG = "USER";

    /**
     * An exception that gets thrown if we have no local user data
     */
    public class ContextNotSetException extends RuntimeException {}

    /**
     * When user data is retrieved from database is ready,
     * it is passed along as a parameter by the interface method.
     * Utilized for: queryUserById, initializeLocalUser, queryUserByName, updateUser
     */
    public interface OnUserReadyListener {
        void onUserReady(User user);
    }

    /**
     * When user datum is retrieved from database is ready,
     * it is passed along as a parameter by the interface method.
     * Utilized for: queryExperimentSubs
     */
    public interface OnUserListReadyListener {
        void onUserListReady(ArrayList<User> users);
    }

    /**
     * Gets the current local user of the device.
     * @return :User (the current user).
     * @Warning getLocalUser might return null if used during init
     */
    public User getLocalUser(){
        return user;
    }

    /**
     * Provide context so that LocalUserManager can fetch the sharedPreference
     * @param context context to be used.
     */
    public void setContext(Context context) {
        pref = context.getSharedPreferences("USER_CONFIG", Context.MODE_PRIVATE);
    }

    /**
     * Get singleton instance of the class
     * @return singleton :UserManager
     */
    public static UserManager getInstance(){
        if (singleton == null){
            singleton = new UserManager();
        }
        return singleton;
    }

    /**
     * Initializes the local user of the device
     * @param callback :OnUserReadyListener (The user data is passed as a parameter of the method in the callback).
     */
    public void initializeLocalUser(OnUserReadyListener callback)  {
        if (pref == null) {
            throw new ContextNotSetException();
        }

        if (pref.contains("User")){
            Gson gson = new Gson();
            user = gson.fromJson(pref.getString("User", ""), User.class);
            updateUser(user, null);
            if (callback != null) {
                callback.onUserReady(user);
            }
        } else{
            // No id currently exist, so we create a new one
            IdGen.genUserId(new IdGen.onIdReadyListener(){
                @Override
                public void onIdReady(String id) {
                    updateUser(new User(id), callback);
                }
            });
        }
    }

    /**
     * Queries the user from Firestore database given the user's id.
     * @param userId :String (ID of the user)
     * @param callback :OnUserReadyListener (The user data is passed as a parameter of the method in the callback).
     */
    @SuppressWarnings("unchecked")
    public void queryUserById(String userId, OnUserReadyListener callback){

        DocumentReference doc = db.collection("Users").document(userId);
        doc.get(Source.DEFAULT).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document!=null &&document.exists() && document.get("Contacts") instanceof Map){

                        callback.onUserReady(userFromSnapshot(document));
                    }

                } else{
                    callback.onUserReady(null);
                }
            }
        });
    }

    /**
     * Queries the user from Firestore database given the user's name.
     * @param name :String (ID of the user)
     * @param callback :OnUserReadyListener (The user data is passed as a parameter of the method in the callback).
     */
    public void queryUserByName(String name, OnUserReadyListener callback){

        db.collection("Users").whereEqualTo("UserName", name).limit(1).get().addOnCompleteListener(task -> {

           if(task.isSuccessful() && callback!= null){
               if (task.getResult().size() == 0){
                   callback.onUserReady(null);
               } else{
                   for(QueryDocumentSnapshot doc: task.getResult()){
                       callback.onUserReady(userFromSnapshot(doc));
                   }
               }
           } else{
               callback.onUserReady(null);
           }
        });
    }

    /**
     * Queries the subscribers of the given experiment.
     * @param expId :String (The user to query experiments for).
     * @param callback :OnUserListReadyListener (The user data is passed as a parameter of the method in the callback).
     * @return void
     */
    @SuppressWarnings("unchecked")
    public void queryExperimentSubs(String expId, OnUserListReadyListener callback){
        db.collection("Experiments").document(expId).get().addOnCompleteListener(
                new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                             ArrayList<String> userIds = (ArrayList<String>) task.getResult().get("SubscriberIDs");

                             if (userIds == null || userIds.size() == 0){
                                 callback.onUserListReady(new ArrayList<User>());
                             } else{
                                 db.collection("Users").whereIn("__name__", userIds).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                     @Override
                                     public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                         ArrayList<User> output = new ArrayList<>();
                                         if(task.isSuccessful()){
                                             for(DocumentSnapshot snap: task.getResult()){
                                                 output.add(userFromSnapshot(snap));
                                             }
                                             callback.onUserListReady(output);
                                         }
                                     }
                                 });
                             }
                        } else {
                            // TODO: error
                            Log.d(TAG, "oh no! WTF");
                        }
                    }
                }
        );
    }

    /**
     * Updates (or creates if not existing) the local user. This method will update the user stored in memory, locally, and in Firestore.
     * If the user's Id already exist, then the exist user document will be updated(replaced).
     * <b>Note:</b> Changing user id or creating new users requires UserReadyCalled to be registered to LocalUserManager
     * <b>Note2:</b> Upon changing user id, user should be given the option to permanently delete the old profile. Then be switched to the new profile
     * @throws ContextNotSetException Throws exception if no context has ever been set for this LocalUserManager
     * @param newUser :User (user to be made or updated)
     * @param callback :OnUserReadyListener (The user data is passed as a parameter of the method in the callback).
     * @return void
     */
    public void updateUser(User newUser, OnUserReadyListener callback){
        if (pref == null){
            throw new ContextNotSetException();
        }
        user = newUser;
        Gson gson = new Gson();
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putString("User", gson.toJson(user));
        prefEditor.apply();

        Map<String, Object> doc = new HashMap<>();
            doc.put("UserDescription", user.getDescription());
            doc.put("UserName", user.getUserName());
                Map<String, Object> contact = new HashMap<>();

                        contact.put("CityName", user.getContactInfo().getCityName());
                        contact.put("Email", user.getContactInfo().getEmail());

        doc.put("Contacts", contact);
        db.collection("Users").document(user.getUserId()).set(doc).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    if(callback != null){
                        callback.onUserReady(user);
                    }
                }
            }
        });
    }

    /**
     * This method returns a User object by constructing it using the data from the document snapshot.
     * @param document :DocumentSnapshot (The Firestore document to retrieve the user details from).
     * @return :User (Constructed using info from document).
     */
    private User userFromSnapshot(DocumentSnapshot document){
        Map<String, Object> contact = (Map<String, Object> )document.get("Contacts");
        // update firebase and other stuff
        String description = document.getString("UserDescription");
        String name = document.getString("UserName");

        // if no contacts assert error
        assert contact != null;
        User temp = new User(name, document.getId(),
                new UserContactInfo(contact.get("CityName").toString(), contact.get("Email").toString()
                ), description);
        return temp;
    }
}