package com.coen390.teamc.cardiograph;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.util.ArrayList;
import java.util.List;

public class ContactsPage extends AppCompatActivity {

    private class contact {
        String mName;
        String mPhone;
        String mPriority;
        String mAction;

        contact (String name, String phone, String priority, String action) {
            mName = name;
            mPhone = phone;
            mPriority = priority;
            mAction = action;
        }
    }

    private DB_Helper myDBHelper;
    private ToolTipView mAddContactToolTipView;
    private ToolTipView mHoldToEditToolTipView;

    private List<String> mContacts;
    private List<contact>  true_Contacts;
    private ArrayAdapter<String> mContactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        myDBHelper = new DB_Helper(this);
    }

    @Override
    protected void onResume(){
        super.onResume();

        FloatingActionButton warning_fab = (FloatingActionButton) findViewById(R.id.add_contact_fab);
        warning_fab.setOnClickListener(new CustomClickLister());

        listAllContacts();

        showAddContactTooltip();

        if (!mContacts.isEmpty()) {
            showHoldToEditTooltip();
        }
    }

    private void showAddContactDialog(View v){
        LayoutInflater inflater = this.getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.add_contact_dialog, null);
        new AlertDialog.Builder(v.getContext())
                .setView(inflator)
                .setTitle("New Contact")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText tv = (EditText) inflator.findViewById(R.id.contact_name);
                        String name = tv.getText().toString();
                        tv = (EditText) inflator.findViewById(R.id.phone_number);
                        String phone = tv.getText().toString();
                        Spinner spn = (Spinner) inflator.findViewById(R.id.priority_spinner);
                        String priority = spn.getSelectedItem().toString();
                        spn = (Spinner) inflator.findViewById(R.id.action_spinner);
                        String action = spn.getSelectedItem().toString();

                        myDBHelper.insertContact(name, phone, priority, action);
                        listAllContacts();
                        showHoldToEditTooltip();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(R.drawable.ic_add_contact_24dp)
                .show();
    }

    private void showEditContactDialog(View v, final String phone_to_delete, contact edit_this){
        LayoutInflater inflater = this.getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.add_contact_dialog, null);
        AlertDialog mEditDialog = new AlertDialog.Builder(v.getContext())
                .setView(inflator)
                .setTitle("Edit Contact")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        EditText tv = (EditText) inflator.findViewById(R.id.contact_name);
                        String name = tv.getText().toString();
                        tv = (EditText) inflator.findViewById(R.id.phone_number);
                        String phone = tv.getText().toString();
                        Spinner spn = (Spinner) inflator.findViewById(R.id.priority_spinner);
                        String priority = spn.getSelectedItem().toString();
                        spn = (Spinner) inflator.findViewById(R.id.action_spinner);
                        String action = spn.getSelectedItem().toString();

                        myDBHelper.removeContactByPhone(phone_to_delete);
                        myDBHelper.insertContact(name, phone, priority, action);
                        listAllContacts();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(R.drawable.ic_add_contact_24dp)
                .show();

        EditText name = (EditText) mEditDialog.findViewById(R.id.contact_name);
        name.setText(edit_this.mName);

        EditText phone = (EditText) mEditDialog.findViewById(R.id.phone_number);
        phone.setText(edit_this.mPhone);

        Spinner prio_spn = (Spinner) mEditDialog.findViewById(R.id.priority_spinner);
        prio_spn.setSelection(Integer.parseInt(edit_this.mPriority) - 1);

        Spinner action_spn = (Spinner) mEditDialog.findViewById(R.id.action_spinner);
        int action_index = 0;
        if (edit_this.mAction.equals("Text")) {
            action_index = 0;
        } else if (edit_this.mAction.equals("Call")) {
            action_index = 1;
        } else if (edit_this.mAction.equals("Both")) {
            action_index = 2;
        }
        action_spn.setSelection(action_index);
    }


    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.add_contact_fab:
                    showAddContactDialog(v);
            }
        }
    }

    private void showAddContactTooltip(){
        if (mAddContactToolTipView != null) {
            mAddContactToolTipView.remove();
        }

        ToolTipRelativeLayout mToolTipFrameLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltipRelativeLayout);

        ToolTip toolTip = new ToolTip()
                .withText(" Add Contact ! ")
                .withColor(Color.parseColor("#ffdada"))
                .withTextColor(Color.BLACK)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);

        mAddContactToolTipView = mToolTipFrameLayout.showToolTipForView(toolTip, findViewById(R.id.add_contact_fab));
    }

    private void hideShowHoldToEditToolTip() {

        if (mHoldToEditToolTipView != null) {
            mHoldToEditToolTipView.remove();
        }
    }
    private void showHoldToEditTooltip(){

        hideShowHoldToEditToolTip();

        ToolTipRelativeLayout mToolTipFrameLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltipRelativeLayout);

        ToolTip toolTip = new ToolTip()
                .withText(" Hold to Edit ! ")
                .withColor(Color.parseColor("#ffdada"))
                .withTextColor(Color.BLACK)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);

        mHoldToEditToolTipView = mToolTipFrameLayout.showToolTipForView(toolTip, findViewById(R.id.invisible_box));
    }

    private void listAllContacts() {
        Cursor cursor = myDBHelper.getAllContacts();
        mContacts = new ArrayList<>();
        true_Contacts = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contactName = new String(cursor.getString(0));
                String contactPhone = new String(cursor.getString(1));
                String contactPriority = new String(cursor.getString(2));
                String contactAction = new String(cursor.getString(3));
                mContacts.add(contactName + " (" + contactPhone + ")" + " - " + contactAction + " - " + contactPriority);
                true_Contacts.add(new contact(contactName, contactPhone, contactPriority, contactAction));
            }while (cursor.moveToNext());
            cursor.close();
        }

        mContactsAdapter = new ArrayAdapter<String>
                (this, R.layout.list_item_contact,R.id.list_item_contact_textview, mContacts);

        ListView listView = (ListView) findViewById(R.id.listview_contacts);
        listView.setAdapter(mContactsAdapter);
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        hideShowHoldToEditToolTip();
        if (v.getId()==R.id.listview_contacts) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.setHeaderTitle(mContacts.get(info.position));
            String[] menuItems = getResources().getStringArray(R.array.contact_menu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.contact_menu);
        String menuItemName = menuItems[menuItemIndex];
        contact selected_contact = true_Contacts.get(info.position);

        if (menuItemName.equals("Delete")) {
            myDBHelper.removeContactByPhone(selected_contact.mPhone);
            listAllContacts();
        }

        if (menuItemName.equals("Edit")) {
            showEditContactDialog(this.findViewById(R.id.contacts_page), selected_contact.mPhone, selected_contact);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
