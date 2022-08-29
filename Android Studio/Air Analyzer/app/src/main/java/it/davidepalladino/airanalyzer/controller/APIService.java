/*
 * This control class provides to connect to the database between API request.
 * Specifically, is possible to get and set information about the room, and to get information about the measures read from the sensors.
 *
 * Copyright (c) 2022 Davide Palladino.
 * All right reserved.
 *
 * @author Davide Palladino
 * @contact davidepalladino@hotmail.com
 * @website https://davidepalladino.github.io/
 * @version 3.0.0
 * @date 28th August, 2022
 *
 */

package it.davidepalladino.airanalyzer.controller;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import it.davidepalladino.airanalyzer.R;
import it.davidepalladino.airanalyzer.model.Authorization;
import it.davidepalladino.airanalyzer.model.MeasuresDateAverage;
import it.davidepalladino.airanalyzer.model.MeasuresDateLatest;
import it.davidepalladino.airanalyzer.model.MeasuresTodayLatest;
import it.davidepalladino.airanalyzer.model.Notification;
import it.davidepalladino.airanalyzer.model.User;
import it.davidepalladino.airanalyzer.model.Room;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static it.davidepalladino.airanalyzer.controller.consts.BroadcastConst.*;
import static it.davidepalladino.airanalyzer.controller.consts.IntentConst.*;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;

public class APIService extends Service {
    public static final String SERVICE_STATUS_CODE = "SERVICE_STATUS_CODE";
    public static final String SERVICE_BODY = "SERVICE_BODY";

    public static boolean isRunning = false;

    private APIRoute api;

    public IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public APIService getService() {
            return APIService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        String baseURL = APIRoute.BASE_URL;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
                .build();

        api = retrofit.create(APIRoute.class);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    /**
     * @brief This method provides to execute the login, saving the token and his type on Authorization object.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void login(@NonNull User user, @NonNull String applicantActivity) {
        Call<Authorization> call = api.login(user);
        call.enqueue(new Callback<Authorization>() {
            @Override
            public void onResponse(Call<Authorization> call, Response<Authorization> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);

                if ((response.code() == 200) && (response.body() != null)) {
                    Authorization.setInstance(response.body());
                } else if ((response.code() == 409) && (response.errorBody() != null)) {
                    try {
                        intentBroadcast.putExtra(SERVICE_BODY, response.errorBody().string());
                    } catch (IOException ignored) { }
                }

                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<Authorization> call, Throwable t) { }
        });
    }

    /**
     * @brief This method provides to execute the sign up.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void signup(User user, String applicantActivity) {
        Call<User> call = api.signup(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);

                if ((response.code() == 200) && (response.body() != null)) {
                    intentBroadcast.putExtra(SERVICE_BODY, (Parcelable) response.body());
                } else if ((response.code() == 409) && (response.errorBody() != null)) {
                    try {
                        intentBroadcast.putExtra(SERVICE_BODY, response.errorBody().string());
                    } catch (IOException ignored) { }
                }

                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }

    /**
     * @brief This method provides to get information about the user authenticate.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getMe(@NonNull String applicantActivity) {
        String authorizationToken = Authorization.getInstance().getAuthorization();

        Call<User> call = api.getMe(authorizationToken);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putExtra(SERVICE_BODY, (Parcelable) response.body());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }









    // FIXME

    /**
     * @brief This method provides to check if the username is already exists on the database. Will be launched a message Broadcast, specifically:
     *  - 201 status code to indicate that the username exists;
     *  - 204 status code to indicate that the username doesn't exist.
     * @param username Username to check.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void checkUsername(String username, String applicantActivity) {
        if (!username.isEmpty()) {
            Call<ResponseBody> call = api.checkUsername(username);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                    intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                    intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                    sendBroadcast(intentBroadcast);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                }
            });
        }
    }

    /**
     * @brief This method provides to check if the username is already exists on the database. Will be launched a message Broadcast, specifically:
     *  - 201 status code to indicate that the email exists;
     *  - 204 status code to indicate that the email doesn't exist.
     * @param email Username to check.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void checkEmail(String email, String applicantActivity) {
        if (!email.isEmpty()) {
            Call<ResponseBody> call = api.checkEmail(email);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                    intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                    intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                    sendBroadcast(intentBroadcast);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                }
            });
        }
    }

    /**
     * @brief This method provides to get the rooms active or not. Will be launched a message Broadcast, specifically:
     *  - JSON { ID, Name, LocalIP } with 200 status code to indicate that the information has been returned;
     *  - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param isActive Active rooms (with "true" value) or not (with "false" value).
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getRooms(String token, boolean isActive, String applicantActivity) {
        Call<ArrayList<Room>> call = api.getRooms(token, isActive ? (byte) 1 : (byte) 0);
        call.enqueue(new Callback<ArrayList<Room>>() {
            @Override
            public void onResponse(Call<ArrayList<Room>> call, Response<ArrayList<Room>> response) {
                ArrayList<Room> listRooms = response.body();

                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putParcelableArrayListExtra(SERVICE_BODY, listRooms);
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ArrayList<Room>> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to rename a specific room. Will be launched a message Broadcast, specifically:
     *  - 200 status code to indicate that the name of room has been changed;
     *  - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param roomID ID of room to rename.
     * @param roomName New name of room.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void renameRoom(String token, byte roomID, String roomName, String applicantActivity) {
        Call<ResponseBody> call = api.renameRoom(token, roomID, roomName);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to activate a specific room. Will be launched a message Broadcast, specifically:
     *  - 200 status code to indicate that the name of room has been activated;
     *  - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param roomID ID of room to activate.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void activateRoom(String token, byte roomID, String applicantActivity) {
        Call<ResponseBody> call = api.activateRoom(token, roomID);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to deactivate a specific room. Will be launched a message Broadcast, specifically:
     *  - 200 status code to indicate that the name of room has been deactivate;
     *  - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param roomID ID of room to activate.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void deactivateRoom(String token, byte roomID, String applicantActivity) {
        Call<ResponseBody> call = api.deactivateRoom(token, roomID);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to get the last measures in actual date (of the client), considering a specific UTC offset, for all rooms active.
     *  Will be launched a message Broadcast, specifically:
     *   - JSON { ID, Name, Time, Temperature, Humidity } with 200 status code to indicate that the information has been returned;
     *   - 204 to indicate that there are not measures;
     *   - 401 status code to indicate that the login had error.
     * @param token Token for the authentication.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getMeasuresTodayLatest(String token, String applicantActivity) {
        Calendar calendar = Calendar.getInstance();

        String date = ManageDatetime.createDateFormat(calendar, getString(R.string.timestamp));
        int utc = ManageDatetime.getUTC(calendar);

        Call<ArrayList<MeasuresTodayLatest>> call = api.getMeasuresTodayLatest(token, date, utc);
        call.enqueue(new Callback<ArrayList<MeasuresTodayLatest>>() {
            @Override
            public void onResponse(Call<ArrayList<MeasuresTodayLatest>> call, Response<ArrayList<MeasuresTodayLatest>> response) {
                ArrayList<MeasuresTodayLatest> listMeasures = response.body();

                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putParcelableArrayListExtra(SERVICE_BODY, listMeasures);
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ArrayList<MeasuresTodayLatest>> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to get the last measures on specific date, considering a specific UTC offset, for a specific room.
     *  Will be launched a message Broadcast, specifically:
     *   - JSON { Time, Temperature, Humidity } with 200 status code to indicate that the information has been returned;
     *   - 204 to indicate that there are not measures.
     *   - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param roomID Room to get the measures.
     * @param date Date in format YYYY-MM-DD.
     * @param utc UTC in format +/-HH:00.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getMeasuresDateLatest(String token, byte roomID, String date, int utc, String applicantActivity) {
        Call<MeasuresDateLatest> call = api.getMeasuresDateLatest(token, roomID, date, utc);
        call.enqueue(new Callback<MeasuresDateLatest>() {
            @Override
            public void onResponse(Call<MeasuresDateLatest> call, Response<MeasuresDateLatest> response) {
                MeasuresDateLatest measuresDateLatest = response.body();

                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putExtra(SERVICE_BODY, measuresDateLatest);
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<MeasuresDateLatest> call, Throwable t) {

            }
        });
    }

    /**
     * @brief This method provides to get the hour average about the measures on specific date, considering a specific UTC offset, for a specific room.
     *  Will be launched a message Broadcast, specifically:
     *   - JSON { Hour, Temperature, Humidity } with 200 status code to indicate that the information has been returned;
     *   - 204 to indicate that there are not measures.
     *   - 401 status code to indicate that the request is not authorized.
     * @param token Token for the authentication.
     * @param roomID Room to get the measures.
     * @param date Date in format YYYY-MM-DD.
     * @param utc UTC in format +/-HH:00.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getMeasuresDateAverage(String token, byte roomID, String date, int utc, String applicantActivity) {
        Call<ArrayList<MeasuresDateAverage>> call = api.getMeasuresDateAverage(token, roomID, date, utc);
        call.enqueue(new Callback<ArrayList<MeasuresDateAverage>>() {
            @Override
            public void onResponse(Call<ArrayList<MeasuresDateAverage>> call, Response<ArrayList<MeasuresDateAverage>> response) {
                ArrayList<MeasuresDateAverage> listMeasures = response.body();

                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putParcelableArrayListExtra(SERVICE_BODY, listMeasures);
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ArrayList<MeasuresDateAverage>> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to get 100 latest notifications.
     *  Will be launched a message Broadcast, specifically:
     *   - JSON { ID, DateAndTime, Type, Name, IsSeen } with 200 status code to indicate that the notifications has been get.
     *   - 204 to indicate that there are not notifications;
     *   - 401 status code to indicate that the request is not authorized. Is expected a JSON format.
     * @param token Token for the authentication.
     * @param utc UTC in format +/-HH:00.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void getNotificationsLatest(String token, int utc, String applicantActivity) {
        Call<ArrayList<Notification>> call = api.getNotificationsLatest(token, utc);
        call.enqueue(new Callback<ArrayList<Notification>>() {
            @Override
            public void onResponse(Call<ArrayList<Notification>> call, Response<ArrayList<Notification>> response) {
                ArrayList<Notification> listNotifications = response.body();

                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                intentBroadcast.putParcelableArrayListExtra(SERVICE_BODY, listNotifications);
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ArrayList<Notification>> call, Throwable t) {
            }
        });
    }

    /**
     * @brief This method provides to change the status of notification, between seen and unseen.
     *  Will be launched a message Broadcast, specifically:
     *   - 200 status code to indicate that the notifications has been changed.
     *   - 401 status code to indicate that the request is not authorized. Is expected a JSON format.
     *   - 406 status code to indicate that the request is not correct;
     * @param token Token for the authentication.
     * @param notifications JSON with the notifications.
     * @param applicantActivity Name of the applicant activity for the broadcast message.
     */
    public void setStatusNotifications(String token, JsonArray notifications, String applicantActivity) {
        Call<ResponseBody> call = api.setStatusNotifications(token, notifications);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Intent intentBroadcast = new Intent(INTENT_BROADCAST);
                intentBroadcast.putExtra(BROADCAST_REQUEST_CODE_APPLICANT_ACTIVITY, applicantActivity);
                intentBroadcast.putExtra(SERVICE_STATUS_CODE, response.code());
                sendBroadcast(intentBroadcast);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }
}