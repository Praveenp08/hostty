package com.example.hostty.Resources.ECE;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.example.hostty.Account.LoginActivity;
import com.example.hostty.R;
import com.example.hostty.Resources.ECE.ECEBranch;
import com.example.hostty.Resources.ECE.ECECourseAdapter;
import com.example.hostty.Resources.CourseClass;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class ECEBranch extends AppCompatActivity {
    private static final String TAG = ECEBranch.class.getSimpleName();
    private ArrayList<CourseClass> eceCourseList;
    private ECECourseAdapter eceECECourseAdapter;

    private DatabaseReference eceCourseDocRef;
    private FirebaseUser eceCourseUser;
    private DatabaseReference eceCourseRef;
    private ShimmerFrameLayout mShimmerViewContainer;

    private TextView emptyCourses;
    private FloatingActionButton eceAddCourse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.branch_listview);
        mShimmerViewContainer = findViewById(R.id.shimmer_view_container);

        eceInit();
        updateCourses();

        eceAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addCourse();
            }
        });

    }

    private void addCourse() {


        final Dialog dialog = new Dialog(ECEBranch.this);
        dialog.setContentView(R.layout.add_course);
        dialog.setTitle(" Add Course ");
        dialog.setCancelable(true);


        final EditText courseNameEt = dialog.findViewById(R.id.course_add_name);
        final EditText courseNoEt = dialog.findViewById(R.id.course_add_no);
        Button addButton = dialog.findViewById(R.id.course_add_btn);


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.setCancelable(false);

                final String name;
                final String no;

                name = courseNameEt.getText().toString().trim();
                no = courseNoEt.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill Course name ", Toast.LENGTH_SHORT).show();
                    courseNameEt.requestFocus();
                    dialog.setCancelable(true);
                    return;
                }
                if (no.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill Course no ", Toast.LENGTH_SHORT).show();
                    courseNoEt.requestFocus();
                    dialog.setCancelable(true);
                    return;
                }

                mShimmerViewContainer.startShimmer();

                Calendar calendar = Calendar.getInstance();
                final String date = new SimpleDateFormat("dd MMM yy h:mm:ss a", Locale.US).format(calendar.getTime());


                final String username = eceCourseUser.getDisplayName();

                CourseClass newCourse = new CourseClass(name, no, date, username, "ECE");


                String userReference = eceCourseRef.push().getKey();
                eceCourseDocRef.child(name).push();

                assert userReference != null;
                eceCourseRef.child(userReference).setValue(newCourse).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to add Course", Toast.LENGTH_SHORT).show();
                        mShimmerViewContainer.stopShimmer();
                        mShimmerViewContainer.setVisibility(View.GONE);
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        CourseClass newCourse = new CourseClass(name, no, date, username, "ECE");
                        eceCourseList.add(newCourse);
                        eceECECourseAdapter.notifyDataSetChanged();

                        if (emptyCourses.getVisibility() == View.VISIBLE) {
                            emptyCourses.setVisibility(View.GONE);
                        }
                        mShimmerViewContainer.stopShimmer();
                        mShimmerViewContainer.setVisibility(View.GONE);
                    }
                });
                dialog.dismiss();


            }
        });

        dialog.show();

    }


    private void updateCourses() {


        mShimmerViewContainer.startShimmer();
        eceCourseRef.limitToLast(20).orderByChild("dateCreated").addListenerForSingleValueEvent(new ValueEventListener() {


            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                if (dataSnapshot.exists()) {
                    eceCourseList.clear();

                    emptyCourses.setVisibility(View.GONE);

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, "onDataChange: reached");
                        CourseClass courses = snapshot.getValue(CourseClass.class);

                        if (courses != null) {
                            eceCourseList.add(courses);
                        }

                    }
                    Collections.reverse(eceCourseList);
                    eceECECourseAdapter.notifyDataSetChanged();

                } else {
                    emptyCourses.setVisibility(View.VISIBLE);
                }
                mShimmerViewContainer.stopShimmer();
                mShimmerViewContainer.setVisibility(View.GONE);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Log.e(TAG, "Failed to read value.", databaseError.toException());
                mShimmerViewContainer.stopShimmer();
                mShimmerViewContainer.setVisibility(View.GONE);
            }
        });


    }

    private void eceInit() {

        emptyCourses = findViewById(R.id.branch_course_empty);
        ListView listView = findViewById(R.id.branch_list_view);
        eceAddCourse = findViewById(R.id.course_add_fab);
        eceCourseList = new ArrayList<>();

        FirebaseAuth eceCourseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase eceCourseInstance = FirebaseDatabase.getInstance();
        DatabaseReference eceCourseRootRef = eceCourseInstance.getReference("Resources");
        eceCourseRef = eceCourseRootRef.child("Courses").child("ECE").getRef();
        eceCourseDocRef = eceCourseRootRef.child("CourseDocs").child("ECE").getRef();
        eceCourseUser = eceCourseAuth.getCurrentUser();


        if (eceCourseUser == null) {
            startActivity(new Intent(ECEBranch.this, LoginActivity.class));
            finish();
        }


        eceECECourseAdapter = new ECECourseAdapter(this, eceCourseList);
        listView.setAdapter(eceECECourseAdapter);
    }

    protected void onResume() {
        super.onResume();

    }

    public boolean onCreateOptionsMenu(Menu menu) {

        androidx.appcompat.app.ActionBar FSportsActionBar = getSupportActionBar();
        assert FSportsActionBar != null;
        FSportsActionBar.setHomeButtonEnabled(true);
        FSportsActionBar.setDisplayHomeAsUpEnabled(true);
        FSportsActionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5cae80")));
        FSportsActionBar.setTitle(Html.fromHtml("<font color='#ffffff'>Ece</font>"));
        return super.onCreateOptionsMenu(menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return true;

    }

}
