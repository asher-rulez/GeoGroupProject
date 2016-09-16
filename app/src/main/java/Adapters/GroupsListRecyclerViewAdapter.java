package Adapters;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import DataModel.Group;
import DataModel.UserToGroupAssignment;
import Utils.FirebaseUtil;
import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 16-Sep-16.
 */
public class GroupsListRecyclerViewAdapter extends RecyclerView.Adapter<GroupsListRecyclerViewAdapter.GroupViewHolder> {
    private static final String MY_TAG = "geog_rvaGroups";

    Context context;
    ArrayList<Group> myGroups;
    Query myGroupsQuery;
    ChildEventListener myGroupsQueryListener;

    public GroupsListRecyclerViewAdapter(Context ctx){
        context = ctx;
        myGroupsQuery = FirebaseUtil.GetMyGroupsQuery(context);
        myGroupsQuery.addChildEventListener(getMyGroupsQueryListener());
    }

    public void clear(){
        if(myGroups != null)
            myGroups.clear();
        if(myGroupsQuery != null)
            myGroupsQuery.removeEventListener(getMyGroupsQueryListener());
        notifyDataSetChanged();
    }

    @Override
    public void onViewRecycled(GroupViewHolder holder) {
        super.onViewRecycled(holder);
        holder.RemoveListener();
    }

    @Override
    public void onViewDetachedFromWindow(GroupViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.RemoveListener();
    }

    private ChildEventListener getMyGroupsQueryListener() {
        if(myGroupsQueryListener == null)
            myGroupsQueryListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    for(Group g : getMyGroups())
                        if(g.getGeneratedID().equals(utga.getGroupID()))
                            return;
                    FirebaseUtil.GetQueryForSingleGroupByGroupKey(context, utga.getGroupID())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot ds) {
                                                                    if(ds.hasChildren()){
                                                                        for(DataSnapshot ds1 : ds.getChildren()){
                                                                            Group group = ds1.getValue(Group.class);
                                                                            getMyGroups().add(group);
                                                                            notifyDataSetChanged();
                                                                        }
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {
                                                                    databaseError.toException().printStackTrace();
                                                                }
                                                            });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    UserToGroupAssignment utga = dataSnapshot.getValue(UserToGroupAssignment.class);
                    for(final Group g : getMyGroups()){
                        if(utga.getGroupID().equals(g.getGeneratedID())){
                            FirebaseUtil.GetQueryForSingleGroupByGroupKey(context, utga.getGroupID())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot ds) {
                                            Group group = ds.getValue(Group.class);
                                            g.setName(group.getName());
                                            notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            databaseError.toException().printStackTrace();
                                        }
                                    });
                            return;
                        }
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Group group = dataSnapshot.getValue(Group.class);
                    for(Group g : getMyGroups()){
                        if(g.getGeneratedID().equals(group.getGeneratedID())){
                            getMyGroups().remove(g);
                            notifyDataSetChanged();
                            return;
                        }
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            };
        return myGroupsQueryListener;
    }

    private ArrayList<Group> getMyGroups(){
        if(myGroups == null)
            myGroups = new ArrayList<>();
        return myGroups;
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_in_list_item, parent, false);
        return new GroupViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(GroupViewHolder holder, int position) {
        holder.SetProperties(getMyGroups().get(position));
    }

    @Override
    public int getItemCount() {
        return getMyGroups().size();
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder{
        Context context;
        Group group;
        TextView tv_title;
        TextView tv_number_of_members;

        Query groupMembersQuery;
        ValueEventListener groupMembersQueryListener;

        public GroupViewHolder(View itemView, Context ctx) {
            super(itemView);
            context = ctx;
            tv_title = (TextView)itemView.findViewById(R.id.tv_group_item_title);
            tv_number_of_members = (TextView)itemView.findViewById(R.id.tv_group_item_number_of_members);
        }

        public void SetProperties(Group group){
            this.group = group;
            tv_title.setText(group.getName());
            RemoveListener();
            groupMembersQuery = FirebaseUtil.GetUsersOfGroupQuery(context, group.getGeneratedID());
            groupMembersQuery.addValueEventListener(getGroupMembersQueryListener());
        }

        public void RemoveListener(){
            if(groupMembersQuery != null)
                groupMembersQuery.removeEventListener(getGroupMembersQueryListener());
        }

        public Group getGroup(){
            return group;
        }

        private ValueEventListener getGroupMembersQueryListener(){
            if(groupMembersQueryListener == null){
                groupMembersQueryListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChildren()){
                            int count = 0;
                            for(DataSnapshot ds : dataSnapshot.getChildren())
                                count++;
                            tv_number_of_members.setText(String.valueOf(count));
                        }
                            tv_number_of_members.setText("");
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                };
            }
            return groupMembersQueryListener;
        }
    }
}
