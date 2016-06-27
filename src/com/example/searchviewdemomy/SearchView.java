package com.example.searchviewdemomy;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.searchviewdemomy.model.CityModel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


@SuppressLint("HandlerLeak")
public class SearchView extends LinearLayout implements View.OnClickListener {

	/**
	 * 输入框
	 */
	private EditText etInput;

	/**
	 * 删除键
	 */
	private ImageView ivDelete;


	/**
	 * 上下文对象
	 */
	private Context mContext;

	/**
	 * 弹出列表
	 */
	private ListView lvTips;

	/**
	 * 提示adapter （推荐adapter）
	 */
	private MyItmeAdapter mHintAdapter;

	/**
	 * 搜索回调接口
	 */
	private SearchViewListener mListener;

	/**
	 * 设置搜索回调接口
	 *
	 * @param listener
	 *            监听者
	 */
	
	private SQLiteDatabase database;

	private ArrayList<String> hintData;

	private DBManager dbManager;

	private TextView tv_item_search;
	
	private  Handler handler=new Handler(){
		
		public void handleMessage(android.os.Message msg) {
			
			switch (msg.what) {
			case 1:
				@SuppressWarnings("unchecked")
				ArrayList<String>selectCityNames=(ArrayList<String>) msg.obj;
				hintData.clear();
				hintData.addAll(selectCityNames);
				mHintAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
		};
	};

	
	public void setSearchViewListener(SearchViewListener listener) {
		mListener = listener;
	}

	public SearchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		LayoutInflater.from(context).inflate(R.layout.search_layout, this);
//		setScrollView(scrollView);
		initViews();
	}

	private void initViews() {
		dbManager = new DBManager(mContext);
		etInput = (EditText) findViewById(R.id.search_et_input);
		ivDelete = (ImageView) findViewById(R.id.search_iv_delete);
		lvTips = (ListView) findViewById(R.id.search_lv_tips);
		lvTips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				// set edit text
				String text = lvTips.getAdapter().getItem(i).toString();
				System.out.println(text+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				etInput.setText(text);
				etInput.setSelection(text.length());
				// hint list view gone and result list view show
				mHintAdapter.notifyDataSetChanged();
				hideListView();
				notifyStartSearching(text);
			}
		});
		initData();
		ivDelete.setOnClickListener(this);
		etInput.addTextChangedListener(new EditChangedListener());
		etInput.setOnClickListener(this);
	}

	private void initData() {
		
		new Thread(){
			@Override
			public void run() {
				//初始化城市列表数据库
				dbManager.openDateBase();
				dbManager.closeDatabase();
				database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/"
									+ DBManager.DB_NAME, null);
			}
		}.start();
		hintData = new ArrayList();
        mHintAdapter=new MyItmeAdapter();
		lvTips.setAdapter(mHintAdapter);
	}

	/**
	 * 通知监听者 进行搜索操作
	 * 
	 * @param text
	 */
	private void notifyStartSearching(String text) {
		if (mListener != null) {
			mListener.onSearch(etInput.getText().toString());
		}
		// 隐藏软键盘
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
	}

	/**
	 * 设置热搜版提示 adapter
	 */
	public void setTipsHintAdapter(MyItmeAdapter adapter) {
		this.mHintAdapter = (MyItmeAdapter) adapter;
		if (lvTips.getAdapter() == null) {
			lvTips.setAdapter(mHintAdapter);
		}
	}


	private class EditChangedListener implements TextWatcher {
		@Override
		public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
			if (!"".equals(charSequence.toString())) {
				ivDelete.setVisibility(VISIBLE);
				hideListView();
			}
		}

		@Override
		public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
			final String input =getEditInput();
			if (!"".equals(charSequence.toString())) {
				ivDelete.setVisibility(VISIBLE);
				lvTips.setVisibility(View.VISIBLE);
				new Thread(){@Override
				public void run() {
					ArrayList<String> selectCityNames = getSelectCityNames(input);
					Message message = Message.obtain();
					message.what=1;
					message.obj=selectCityNames;
					handler.sendMessage(message);
				}}.start();
				
				
				// 更新autoComplete数据
				if (mListener != null) {
					mListener.onRefreshAutoComplete(charSequence + "");
				}
			} else {
//				ShowToastUtils.Show(mContext, "输入框为空了");
//				ivDelete.setVisibility(GONE);
				if (mHintAdapter != null) {
					lvTips.setAdapter(mHintAdapter);
				}
				hideListView();
			}
		}
		@Override
		public void afterTextChanged(Editable editable) {
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.search_et_input:
			break;
		case R.id.search_iv_delete:
			etInput.setText("");
			ivDelete.setVisibility(GONE);
			break;
		default:
			break;
		}
	}
private void hideListView() {
	hintData.clear();
	lvTips.setVisibility(View.GONE);
}
	/**
	 * search view回调方法
	 */
	public interface SearchViewListener {

		/**
		 * 更新自动补全内容
		 *
		 * @param text
		 *            传入补全后的文本
		 */
		void onRefreshAutoComplete(String text);

		/**
		 * 开始搜索
		 *
		 * @param text
		 *            传入输入框的文本
		 */
		void onSearch(String text);

	}
	public String getEditInput() {
		String text = etInput.getText().toString().trim();
		return text;
	}
    private ArrayList<String> getSelectCityNames(String con) {
		ArrayList<String> names = new ArrayList<String>();
		//判断查询的内容是不是汉字
		Pattern p_str = Pattern.compile("[\\u4e00-\\u9fa5]+");
		Matcher m = p_str.matcher(con);
		String sqlString = null;
		if (m.find() && m.group(0).equals(con)) {
			sqlString = "SELECT * FROM T_city WHERE AllNameSort LIKE " + "\""
					+ con + "%" + "\"" + " ORDER BY CityName";
		} else {
			sqlString = "SELECT * FROM T_city WHERE NameSort LIKE " + "\""
					+ con + "%" + "\"" + " ORDER BY CityName";
		}
		Cursor cursor = database.rawQuery(sqlString, null);
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			CityModel cityModel = new CityModel();
			cityModel.setCityName(cursor.getString(cursor
					.getColumnIndex("AllNameSort")));
			cityModel.setNameSort(cursor.getString(cursor
					.getColumnIndex("CityName")));
			names.add(cityModel.getCityName());
		}
		cursor.close();
		return names;
	}

	/**
	 * 从数据库获取城市数据
	 * 
	 * @return
	 */
	private ArrayList<CityModel> getCityNames() {
		ArrayList<CityModel> names = new ArrayList<CityModel>();
		Cursor cursor = database.rawQuery(
				"SELECT * FROM T_city ORDER BY CityName", null);
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			CityModel cityModel = new CityModel();
			cityModel.setCityName(cursor.getString(cursor
					.getColumnIndex("AllNameSort")));
			cityModel.setNameSort(cursor.getString(cursor
					.getColumnIndex("CityName")));
			names.add(cityModel);
		}
		cursor.close();
		return names;
	}

	class MyItmeAdapter extends BaseAdapter
	{
		@Override
		public int getCount() {
			return hintData.size();
		}

		@Override
		public Object getItem(int position) {
			return hintData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		public  MyItmeAdapter getAdapter()
		{
			return this;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder=null ;
			if (convertView==null) {
				if (holder==null) {
					holder=new Holder();
				}
				convertView=View.inflate(mContext, R.layout.item_searchlocation, null);
				holder.location = (TextView) convertView.findViewById(R.id.tv_item_search);
				convertView.setTag(holder);
//				tv_item_search.setText(hintData.get(position));
			}else {
				holder = (Holder) convertView.getTag();
			}
			holder.location.setText(hintData.get(position));
			return convertView;
		}
		
	}
	class Holder
	{
		private TextView location;
	}
}
