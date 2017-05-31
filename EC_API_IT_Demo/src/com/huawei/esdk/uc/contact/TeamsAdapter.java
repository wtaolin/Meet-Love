package com.huawei.esdk.uc.contact;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.huawei.data.PersonalTeam;
import com.huawei.esdk.uc.R;

public class TeamsAdapter extends BaseAdapter
{

    private List<PersonalTeam> personalTeams = new ArrayList<PersonalTeam>();

    private LayoutInflater mInflater;

    public TeamsAdapter(Context context, List<PersonalTeam> personalTeams)
    {
        if (null != personalTeams)
        {
            this.personalTeams = personalTeams;
        }

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        return personalTeams.size();
    }

    @Override
    public Object getItem(int arg0)
    {
        return personalTeams.get(arg0);
    }

    @Override
    public long getItemId(int arg0)
    {
        return arg0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2)
    {
        View view = mInflater.inflate(R.layout.team_list_item, null);

        TextView tvTeamTitle = (TextView) view.findViewById(R.id.teamtitle);

        PersonalTeam team = personalTeams.get(arg0);

        tvTeamTitle.setText(team.getTeamName());

        return view;
    }

}
