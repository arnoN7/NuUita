package example.com.nuuita;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRole;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class TodoListActivity extends Activity {

    //TodoListFragment todoListFragment;
    public static final int LOGIN_ACTIVITY_CODE = 100;
    public static final int EDIT_ACTIVITY_CODE = 200;
    public static String EMAIL_KEY = "email";
    public static String USER_KEY = "user";
    private DrawerLayout mDrawerLayout;
    private ListView mNavigationMenu;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private List<TodoListFragment> fragments;
    private ActionBarDrawerToggle mDrawerToggle;
    private ImageButton mAddTodoList;
    private ArrayAdapter<String> navigationMenuAdapter;
    private RelativeLayout mLeftDrawer;
    private TodoListFragment currentFragment;

    public TodoListFragment getCurrentFragment(){
        return currentFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_list_activity);

        // If User is not Logged In Start Activity Login
        if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            //Go to login activity
            goToLoginActivity();
            finish();
        } else {
            mTitle = mDrawerTitle = getTitle();
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mNavigationMenu = (ListView) findViewById(R.id.list_navigation_menu);
            mAddTodoList = (ImageButton) findViewById(R.id.add_todo_list);
            mLeftDrawer = (RelativeLayout) findViewById(R.id.left_drawer);
            //Get TodoLists Names
            fragments = initTodoListRoles();

            //Associate Installation with currentUser
            // Associate the device with a user
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
            if (installation == null || installation.get(USER_KEY) == null ||
                    (!((ParseUser)installation.get(USER_KEY)).getEmail().equals(ParseUser.getCurrentUser().getEmail()))) {
                installation.put(USER_KEY,ParseUser.getCurrentUser());
                installation.saveInBackground();
                Log.d("Installation", "Save New User : " + ParseUser.getCurrentUser().getEmail());
            }
            // set a custom shadow that overlays the main content when the drawer opens
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            // Set the adapter for the list view
            navigationMenuAdapter = new ArrayAdapter<String>(this,
                    R.layout.drawer_list_item, getTodoListsNames(fragments));
            mNavigationMenu.setAdapter(navigationMenuAdapter);
            AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectItem(position);
                }
            };
            mNavigationMenu.setOnItemClickListener(listener);

            // enable ActionBar app icon to behave as action to toggle nav drawer
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);

            // ActionBarDrawerToggle ties together the the proper interactions
            // between the sliding drawer and the action bar app icon
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    getActionBar().setTitle(mTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView) {
                    getActionBar().setTitle(mDrawerTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };
            mAddTodoList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Make sure there's a valid user, anonymous
                    // or regular
                    AlertDialog.Builder alert = new AlertDialog.Builder(TodoListActivity.this);

                    alert.setTitle("Ajouter une liste");
                    alert.setMessage("Donnez un nom à cette nouvelle liste");

                    // Set an EditText view to get user input
                    final EditText input = new EditText(TodoListActivity.this);
                    InputFilter filter = new InputFilter() {
                        @Override
                        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                            for (int i = start; i < end; i++) {
                                if (!Character.isLetterOrDigit(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                                    return "";
                                }
                            }
                            return null;
                        }
                    };
                    input.setFilters(new InputFilter[]{filter});
                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final String newTodoList = input.getText().toString();
                            //TODO check if a todolist with the same name does not exist
                            final int index = fragments.size();
                            navigationMenuAdapter.insert(newTodoList, index);
                            ParseACL roleACL = new ParseACL();
                            roleACL.setReadAccess(ParseUser.getCurrentUser(),true);
                            roleACL.setWriteAccess(ParseUser.getCurrentUser(), true);
                            ParseRole todoListRole = new ParseRole(Utils.getRoleName(newTodoList, ParseUser.getCurrentUser()), roleACL);
                            todoListRole.put(Todo.LIST_NAME_KEY, newTodoList);
                            todoListRole.put(Todo.AUTHOR_KEY, ParseUser.getCurrentUser());
                            todoListRole.getUsers().add(ParseUser.getCurrentUser());
                            TodoListFragment newFragment = TodoListFragment.newInstance(todoListRole);
                            fragments.add(newFragment);
                            try {
                                todoListRole.save();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            selectItem(index);
                            Toast listCreated = Toast.makeText(getApplication(), getString(R.string.new_list_created, newTodoList), Toast.LENGTH_LONG);
                            ParsePush.subscribeInBackground(todoListRole.getName());
                            listCreated.show();
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    alert.show();
                }
            });
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerLayout.openDrawer(mLeftDrawer);
        }
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void displayAlertDialog(String title, String message, DialogInterface.OnClickListener positiveButtonListener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(TodoListActivity.this);

        alert.setTitle(title);
        alert.setMessage(message);

        // Set an EditText view to get user input
        final EditText input = new EditText(TodoListActivity.this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String newTodoList = input.getText().toString();
                //TODO check if a todolist with the same name does not exist
                int index = fragments.size();
                navigationMenuAdapter.insert(newTodoList, index);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private List<TodoListFragment> initTodoListRoles() {
        ParseQuery<ParseRole> query = ParseRole.getQuery();
        List<ParseRole> requestedRoles = null;
        List<TodoListFragment> fragments = new ArrayList<>();
        try {
            requestedRoles = query.find();
            for (int i = 0; i < requestedRoles.size(); i++) {
                TodoListFragment fragment = TodoListFragment.newInstance(requestedRoles.get(i));
                fragments.add(fragment);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return fragments;
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = fragments.get(position);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        this.currentFragment = (TodoListFragment) fragment;
        ft.replace(R.id.content_frame, fragment);
        ft.commit();

        // update selected item title, then close the drawer
        setTitle(((TodoListFragment) fragment).getTodoListRole().getString(Todo.LIST_NAME_KEY));
        mDrawerLayout.closeDrawer(mLeftDrawer);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean realUser = !ParseAnonymousUtils.isLinked(ParseUser
                .getCurrentUser());
        //menu.findItem(R.id.action_login).setVisible(!realUser);
//        menu.findItem(R.id.action_logout).setVisible(realUser);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.todo_list, menu);
        return true;
    }

    /*private void openEditView(Todo todo) {
        Intent i = new Intent(this, NewTodoActivity.class);
        i.putExtra("ID", todo.getUuidString());
        startActivityForResult(i, EDIT_ACTIVITY_CODE);
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            AlertDialog.Builder alert = new AlertDialog.Builder(TodoListActivity.this);
            List<ParseUser> sharedUsers = null;
            try {
                sharedUsers = currentFragment.getTodoListRole().getUsers().getQuery().find();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            alert.setTitle("Partager la liste \""+currentFragment.getTodoListName()+"\"");
            String message = getString(R.string.shareWith);
            if (sharedUsers.size() == 1) {
                message += "personne";
            } else {
                int nb_shares = 0;
                for (int i = 0; i < sharedUsers.size(); i++) {
                    //dont print current User
                    if(!sharedUsers.get(i).equals(ParseUser.getCurrentUser())) {
                        if(nb_shares == sharedUsers.size() - 2) {
                            message+=" et";
                        } else if (nb_shares != 0 ) {
                            message += ",";
                        }

                        message += " " + sharedUsers.get(i).getUsername();
                        nb_shares ++;
                        if (i == sharedUsers.size()) {
                            message += ".";
                        }
                    }
                }
            }
            alert.setMessage(message + "\n \n" + getString(R.string.EmailPartageW));

            // Set an EditText view to get user input
            final EditText input = new EditText(TodoListActivity.this);
            alert.setView(input);
            input.setHint("Email");
            input.setRawInputType(33);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String email = input.getText().toString();
                    List<ParseUser> users = null;
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo(EMAIL_KEY, email);
                    try {
                        users = query.find();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if(users.size() > 0)
                    {
                        currentFragment.shareListWithUser(users.get(0));
                        Utils.sendPushToUser(users.get(0), ParseUser.getCurrentUser().get(LoginActivity.USERNAME_KEY) +
                                " a partagé la liste " + currentFragment.getTodoListName() + " avec vous.");
                    }

                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }

        if (item.getItemId() == R.id.action_logout) {
            // Log out the current user
            ParseUser.logOut();
            // Update the logged in label info
            //TODO uncomment line under
            //todoListFragment.updateLoggedInInfo();
            // Clear the view
            //todoList.clear();
            // Unpin all the current objects
            /*ParseObject
                    .unpinAllInBackground(TodoListApplication.TODO_GROUP_NAME);*/
            goToLoginActivity();
            //ParseLoginBuilder builder = new ParseLoginBuilder(this);
            //startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
        }
        if (item.getItemId() == R.id.action_delete) {
            AlertDialog.Builder alert = new AlertDialog.Builder(TodoListActivity.this);
            final ParseRole  roleToDelete = currentFragment.getTodoListRole();
            String todoListName = roleToDelete.getString(Todo.LIST_NAME_KEY);

            alert.setTitle("Supprimer la liste: " + todoListName);
            alert.setMessage("Voulez-vous vraiment supprimer la liste " + todoListName + "?");

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    for (int i = 0; i < fragments.size(); i++) {
                        if(fragments.get(i).getTodoListRole().getName().equals(roleToDelete.getName())) {
                            fragments.remove(i);
                            navigationMenuAdapter.remove(roleToDelete.getString(Todo.LIST_NAME_KEY));
                        }
                    }
                    currentFragment.deleteList();
                    if (fragments.size() > 0) {
                        selectItem(0);
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }

        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    public List<String> getTodoListsNames (List<TodoListFragment> fragments) {
        List<String> names = null;
        if (fragments != null) {
            names =new ArrayList<String>();
            for (int i = 0; i < fragments.size(); i++) {
                names.add(fragments.get(i).getTodoListRole().getString(Todo.LIST_NAME_KEY));
            }
        }
        return names;
    }
}
