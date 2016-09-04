package com.easyfitness;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.easyfitness.DAO.DAOProfil;
import com.easyfitness.DAO.DAOWeight;
import com.easyfitness.DAO.Profil;
import com.easyfitness.DAO.Weight;
import com.easyfitness.graph.Graph;
import com.easyfitness.utils.DateConverter;
import com.easyfitness.utils.ExpandedListView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;


public class ProfilFragment extends Fragment  implements OnDateSetListener {
	private String name;
	private int id;

	Button addWeightButton = null;
	EditText weightEdit = null;
	EditText dateEdit = null;
	TextView resumeText = null;
	TextView profilText = null;
	ExpandedListView weightList = null;

	private LineChart mChart = null;
	private Graph mGraph = null;

	private DAOWeight mWeightDb = null;
	private DAOProfil mDb = null;

	MainActivity mActivity = null;


	/**
	 * Create a new instance of DetailsFragment, initialized to
	 * show the text at 'index'.
	 */
	public static ProfilFragment newInstance(String name, int id) {
		ProfilFragment f = new ProfilFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putString("name", name);
		args.putInt("id", id);
		f.setArguments(args);

		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.tab_profil, container, false);

		addWeightButton = (Button) view.findViewById(R.id.buttonAddWeight);
		weightEdit = (EditText) view.findViewById(R.id.editWeight);
		resumeText = (TextView) view.findViewById(R.id.textResume);
		profilText = (TextView) view.findViewById(R.id.textProfil);
		dateEdit= (EditText) view.findViewById(R.id.profilEditDate);
		weightList= (ExpandedListView) view.findViewById(R.id.listWeightProfil);

		/* Initialisation serie */

		/* Initialisation des boutons */
		addWeightButton.setOnClickListener(onClickAddWeight);
		dateEdit.setOnClickListener(clickDateEdit);
		dateEdit.setOnFocusChangeListener(focusDateEdit);
		weightList.setOnItemLongClickListener(itemlongclickDeleteRecord);

		/* Initialisation des evenements */

		// Add the other graph
		mChart = (LineChart) view.findViewById(R.id.weightChart);
		mChart.setDescription("");
		mGraph = new Graph(mChart, getResources().getText(R.string.weightLabel).toString());
		mWeightDb = new DAOWeight(view.getContext());
		
		// Set Initial text
		dateEdit.setText(DateConverter.currentDate());

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		refreshData();
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		this.mActivity = (MainActivity) activity;
	}

	private OnClickListener onClickAddWeight = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if  (!weightEdit.getText().toString().isEmpty()) {
				
				Date date = DateConverter.editToDate(dateEdit.getText().toString());
				
				mWeightDb.addWeight(date, Float.valueOf(weightEdit.getText().toString()), getProfil());
				refreshData();
				weightEdit.setText("");
			}
		}
	};
	
	private OnClickListener clickDateEdit = new OnClickListener() {
		@Override
		public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				DatePickerDialogFragment mDateFrag = new DatePickerDialogFragment(getFragment());
				mDateFrag.show(ft, "dialog");			
		}
	}; 
	
	private OnFocusChangeListener focusDateEdit = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus == true) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				DatePickerDialogFragment mDateFrag = new DatePickerDialogFragment(getFragment());
				mDateFrag.show(ft, "dialog");			
			}
		}
	}; 
	
	private void DrawGraph(List<Weight> valueList) {
		
		// Recupere les enregistrements
		if (valueList.size() < 1) { mChart.clear(); return; }
		// TODO normalement, je devrais faire un getWeightList sur l'objet Profil. 

		ArrayList<String> xVals = new ArrayList<String>();	
		ArrayList<Entry> yVals = new ArrayList<Entry>();

		// Draw second graph
		long maxDate = -1;
		long minDate = -1;

		for (int i = 0; i<valueList.size();i++) {
			long tmpDate = valueList.get(i).getDate().getTime();
			if (maxDate == -1)  maxDate = tmpDate;
			if (minDate == -1)  minDate = tmpDate;

			if (tmpDate > maxDate) maxDate = tmpDate;
			if (tmpDate < minDate) minDate = tmpDate;
		}
		
		for (long i = minDate; i<=maxDate;i=i+86400000) {
			SimpleDateFormat dt1 = new SimpleDateFormat("dd-MM-yy");
			//dt1.setTimeZone(TimeZone.getTimeZone("GMT")); // On se recalle sur le GMT car le GetTime est en GMT.
			xVals.add(dt1.format(i));
		}

		for (int i = 0; i<valueList.size();i++) {
			Entry value = new Entry(valueList.get(i).getWeight(), (int)((valueList.get(i).getDate().getTime()-minDate)/86400000));
			yVals.add(value);		
		}
		
		mGraph.draw(xVals, yVals);
	}
	
	/*  */
	private void FillRecordTable(List<Weight> valueList) {
		Cursor oldCursor = null;

		if(valueList.isEmpty()) {
			//Toast.makeText(getActivity(), "No records", Toast.LENGTH_SHORT).show();    
			weightList.setAdapter(null);
		} else {
			// ...
			if ( weightList.getAdapter() == null ) {
				ProfilWeightCursorAdapter mTableAdapter = new ProfilWeightCursorAdapter (this.getView().getContext(), mWeightDb.GetCursor(), 0);
				weightList.setAdapter(mTableAdapter);
			} else {
				oldCursor = ((ProfilWeightCursorAdapter)weightList.getAdapter()).swapCursor(mWeightDb.GetCursor());
				if (oldCursor!=null)
					oldCursor.close();
			}
		}
	}
	
	private OnItemLongClickListener itemlongclickDeleteRecord = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> listView, View view,
				int position, long id) {

			// Get the cursor, positioned to the corresponding row in the result set
			//Cursor cursor = (Cursor) listView.getItemAtPosition(position);

			final long selectedID = id;
			
			String[] profilListArray = new String[2];
			profilListArray[0] = getActivity().getResources().getString(R.string.DeleteLabel);
			profilListArray[1] = getActivity().getResources().getString(R.string.ShareLabel);


			AlertDialog.Builder itemActionbuilder = new AlertDialog.Builder(getActivity());
			itemActionbuilder.setTitle("").setItems(profilListArray, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
					switch (which) {
					// Delete
					case 0 : 
						mWeightDb.deleteMeasure(selectedID);

						refreshData();

						Toast.makeText(getActivity(), "Removed weight id " + selectedID, Toast.LENGTH_SHORT).show();//TODO change static string
						
						break;
					// Share
					case 1:
						Toast.makeText(getActivity(), "Share soon available", Toast.LENGTH_SHORT).show();//TODO change static string
					break;
					default:
					}
				}				
			});
			itemActionbuilder.show();
			
			return true;
		}
	};
	
	public void onDateSet(DatePicker view, int year,
			int month, int day) {
		// Do something with the date chosen by the user
		dateEdit.setText(DateConverter.dateToString(year, month+1, day));
	}

	public String getName() { 
		return getArguments().getString("name");
	}

	public int getFragmentId() { 
		return getArguments().getInt("id", 0);
	}

	private DAOWeight getWeightDB() {
		return mWeightDb;
	}


	private DAOProfil getDB() {
		return mDb;
	}

	private void refreshData(){
		View fragmentView = getView();
		if(fragmentView != null) {
			if (getProfil() != null) {
				this.profilText.setText(getProfil().getName());
				this.resumeText.setText("");
				List<Weight> valueList = mWeightDb.getWeightList(getProfil()); 
				DrawGraph(valueList);
				// update table
				FillRecordTable(valueList);
			}
		}
	}

	private Profil getProfil()
	{
		return ((MainActivity)getActivity()).getCurrentProfil();
	}
	

	public Fragment getFragment() {
		return this;
	}

	@Override
	public void onHiddenChanged (boolean hidden) {
		if (!hidden) refreshData();
	}
}