package com.example.meme;

import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TimersActivity extends ListActivity {
    private TimersDAO dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme_database);

        dataSource = new TimersDAO(this);
        dataSource.open();

        List<TimersModel> values = dataSource.getAllEntries();
        ArrayAdapter<TimersModel> adapter = new ArrayAdapter<TimersModel>(this,
                android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TimersModel timersModel = (TimersModel) l.getItemAtPosition(position);
        String toast = "Time Instant: " + timersModel.getTimeInstant() + "\n"
                + "MT: " + timersModel.getMtArray() + "\n" + "RT: "
                + timersModel.getRtArray() + "\n" + "DeltaT: {"
                + timersModel.getDeltatTArray() + "}\n"
                + MemeMainActivity.predict(timersModel);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Time Instant: " + timersModel.getTimeInstant());
        dialog.setPositiveButton("Close", null);
        dialog.setMessage(toast);
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataSource.close();
    }
}
