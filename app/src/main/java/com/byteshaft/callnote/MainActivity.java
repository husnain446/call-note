package com.byteshaft.callnote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import static com.byteshaft.callnote.IncomingCallListener.Note;

public class MainActivity extends ActionBarActivity implements Switch.OnCheckedChangeListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private Helpers mHelpers;
    private DataBaseHelpers mDbHelpers;
    private ArrayList<String> arrayList;
    private ListView listView;
    private TextView textViewTitle;
    private OverlayHelpers mOverlayHelpers;
    private Switch mToggleSwitch;
    private ArrayAdapter<String> mModeAdapter;
    private DataBaseHelpers dataBaseHelpers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#689F39")));
        textViewTitle = (TextView) findViewById(R.id.title);
        mHelpers = new Helpers(getApplicationContext());
        dataBaseHelpers = new DataBaseHelpers(getApplicationContext());
        mToggleSwitch = (Switch) findViewById(R.id.aSwitch);
        mDbHelpers = new DataBaseHelpers(getApplicationContext());
        mToggleSwitch.setOnCheckedChangeListener(this);
        mOverlayHelpers = new OverlayHelpers(getApplicationContext());
        if (dataBaseHelpers.isEmpty()) {
            showNoNoteFoundDialog();
        }
        if (!AppGlobals.PREMIUM) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .setRequestAgent("android_studio:ad_template").build();
            adView.loadAd(adRequest);
        }
    }

    private void showNoNoteFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome!");
        builder.setMessage("Would you like to add your first note?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mToggleSwitch.setChecked(mHelpers.isServiceSettingEnabled());
        if (mHelpers.isServiceSettingEnabled()) {
            mToggleSwitch.setText("Notes Active");
            mToggleSwitch.setTextColor(Color.BLACK);
        } else {
            mToggleSwitch.setText("All Notes OFF");
            mToggleSwitch.setTextColor(Color.RED);
        }
        arrayList = mDbHelpers.getAllPresentNotes();
        mModeAdapter = new NotesArrayList(this, R.layout.row, arrayList);
        listView = (ListView) findViewById(R.id.listView_main);
        listView.setAdapter(mModeAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setDivider(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_addNote:
                if (dataBaseHelpers.getNotesCount() >= 3 && !AppGlobals.PREMIUM) {
                    String message = "You cannot add more than 3 Notes in free version " +
                            "Upgrade to premium";
                    String title = "Notes limit";
                    mHelpers.showUpgradeDialog(MainActivity.this, title, message);
                } else {
                    startActivity(new Intent(this, NoteActivity.class));
                }
                break;
            case R.id.upgrade_button:
                String dialogMessage = "Do you want to upgrade?";
                mHelpers.showUpgradeDialog(MainActivity.this, "Upgrade", dialogMessage);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#689F39")));
        MenuItem item = menu.findItem(R.id.upgrade_button);
        if (AppGlobals.PREMIUM) {
            item.setVisible(false);
        } else {
            item.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            startService(new Intent(this, OverlayService.class));
            mToggleSwitch.setText("Notes Active");
            mToggleSwitch.setTextColor(Color.BLACK);
        } else {
            stopService(new Intent(this, OverlayService.class));
            mToggleSwitch.setText("All Notes OFF");
            mToggleSwitch.setTextColor(Color.RED);
        }
        mHelpers.saveServiceStateEnabled(isChecked);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note_title", arrayList.get(position));
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
        System.out.println(parent.getItemAtPosition(position));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Delete");
        builder.setMessage("Are you sure?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                DataBaseHelpers dataBaseHelpers = new DataBaseHelpers(getApplicationContext());
                dataBaseHelpers.deleteItem(SqliteHelpers.NOTES_COLUMN, (String)
                        parent.getItemAtPosition(position));
                mModeAdapter.remove(mModeAdapter.getItem(position));
                mModeAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    private String getDirectionThumbnail(String title) {
        String uriBase = "android.resource://com.byteshaft.callnote/";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int noteShowPreference = preferences.getInt(title, Note.TURN_OFF);
        if (AppGlobals.PREMIUM) {
            return getDirectionIconForPremium(uriBase, noteShowPreference);
        } else {
            return getDirectionIconForTrial(uriBase, noteShowPreference);
        }
    }

    private String getDirectionIconForTrial(String uriBase, int notePreference) {
        switch (notePreference) {
            case Note.SHOW_INCOMING_CALL:
                return uriBase + R.drawable.incoming_call;
            case Note.TURN_OFF:
                return uriBase + R.drawable.off;
            default:
                return uriBase + R.drawable.off;
        }
    }

    @NonNull
    private String getDirectionIconForPremium(String uriBase, int noteShowPreference) {
        switch (noteShowPreference) {
            case Note.SHOW_INCOMING_CALL:
                return uriBase + R.drawable.incoming_call;
            case Note.SHOW_OUTGOING_CALL:
                return uriBase + R.drawable.outgoing_call;
            case Note.SHOW_INCOMING_OUTGOING:
                return uriBase + R.drawable.incoming_outgoing_call;
            default:
                return uriBase + R.drawable.off;
        }
    }

    static class ViewHolder {
        public TextView title;
        public ImageView character;
        public ImageView direction;
    }

    class NotesArrayList extends ArrayAdapter<String> {

        public NotesArrayList(Context context, int resource, ArrayList<String> videos) {
            super(context, resource, videos);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.row, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.FilePath);
                holder.character = (ImageView) convertView.findViewById(R.id.Thumbnail);
                holder.direction = (ImageView) convertView.findViewById(R.id.note_direction);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            String title = arrayList.get(position);
            holder.title.setText(title);
            holder.character.setImageURI(Uri.parse(mDbHelpers.getIconLinkForNote(title)));
            holder.direction.setImageURI(Uri.parse(getDirectionThumbnail(title)));
            return convertView;
        }
    }
}