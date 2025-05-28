package com.damors.zuji;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.adapter.TimelineFootprintAdapter;
import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.viewmodel.FootprintViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 足迹搜索页面
 * 允许用户按照关键词、城市或分类搜索足迹记录
 */
public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Spinner filterTypeSpinner;
    private RecyclerView resultsRecyclerView;
    private TextView noResultsTextView;
    
    private FootprintViewModel viewModel;
    private TimelineFootprintAdapter adapter;
    private List<FootprintEntity> allFootprints = new ArrayList<>();
    private List<FootprintEntity> filteredFootprints = new ArrayList<>();
    
    private static final int FILTER_ALL = 0;
    private static final int FILTER_DESCRIPTION = 1;
    private static final int FILTER_CITY = 2;
    private static final int FILTER_CATEGORY = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
        
        // 初始化视图
        initViews();
        
        // 设置过滤器下拉列表
        setupFilterSpinner();
        
        // 设置搜索文本变化监听器
        setupSearchTextWatcher();
        
        // 加载所有足迹数据
        loadAllFootprints();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        searchEditText = findViewById(R.id.edit_text_search);
        filterTypeSpinner = findViewById(R.id.spinner_filter_type);
        resultsRecyclerView = findViewById(R.id.recycler_view_results);
        noResultsTextView = findViewById(R.id.text_view_no_results);
        
        // 设置RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimelineFootprintAdapter(this, filteredFootprints);
        resultsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * 设置过滤器下拉列表
     */
    private void setupFilterSpinner() {
        String[] filterTypes = {"全部", "描述", "城市", "分类"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterTypeSpinner.setAdapter(adapter);
        
        // 设置选择监听器
        filterTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 当选择变化时，重新过滤结果
                filterFootprints(searchEditText.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何处理
            }
        });
    }
    
    /**
     * 设置搜索文本变化监听器
     */
    private void setupSearchTextWatcher() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不做任何处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 当文本变化时，重新过滤结果
                filterFootprints(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不做任何处理
            }
        });
    }
    
    /**
     * 加载所有足迹数据
     */
    private void loadAllFootprints() {
        viewModel.getAllFootprints().observe(this, footprints -> {
            if (footprints != null) {
                allFootprints = footprints;
                // 初始加载时显示所有足迹
                filterFootprints(searchEditText.getText().toString());
            }
        });
    }
    
    /**
     * 根据搜索关键词和过滤类型过滤足迹
     * @param query 搜索关键词
     */
    private void filterFootprints(String query) {
        filteredFootprints.clear();
        
        if (query.isEmpty() && filterTypeSpinner.getSelectedItemPosition() == FILTER_ALL) {
            // 如果搜索框为空且选择了"全部"，显示所有足迹
            filteredFootprints.addAll(allFootprints);
        } else {
            // 根据过滤类型和关键词过滤
            int filterType = filterTypeSpinner.getSelectedItemPosition();
            String queryLower = query.toLowerCase();
            
            for (FootprintEntity footprint : allFootprints) {
                boolean match = false;
                
                switch (filterType) {
                    case FILTER_ALL:
                        // 在所有字段中搜索
                        match = (footprint.getDescription() != null && footprint.getDescription().toLowerCase().contains(queryLower)) ||
                               (footprint.getCityName() != null && footprint.getCityName().toLowerCase().contains(queryLower)) ||
                               (footprint.getCategory() != null && footprint.getCategory().toLowerCase().contains(queryLower)) ||
                               (footprint.getLocationName() != null && footprint.getLocationName().toLowerCase().contains(queryLower));
                        break;
                    case FILTER_DESCRIPTION:
                        // 仅在描述中搜索
                        match = footprint.getDescription() != null && footprint.getDescription().toLowerCase().contains(queryLower);
                        break;
                    case FILTER_CITY:
                        // 仅在城市中搜索
                        match = footprint.getCityName() != null && footprint.getCityName().toLowerCase().contains(queryLower);
                        break;
                    case FILTER_CATEGORY:
                        // 仅在分类中搜索
                        match = footprint.getCategory() != null && footprint.getCategory().toLowerCase().contains(queryLower);
                        break;
                }
                
                if (match) {
                    filteredFootprints.add(footprint);
                }
            }
        }
        
        // 更新适配器
        adapter.setFootprints(filteredFootprints);
        
        // 显示或隐藏"无结果"提示
        if (filteredFootprints.isEmpty()) {
            noResultsTextView.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            noResultsTextView.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 单元测试方法
     * 测试搜索过滤功能是否正确
     * @return 是否测试通过
     */
    public boolean testSearchFilter() {
        try {
            // 创建测试数据
            List<FootprintEntity> testData = new ArrayList<>();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setId(1);
            footprint1.setDescription("北京旅游");
            footprint1.setCityName("北京");
            footprint1.setCategory("旅游");
            
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setId(2);
            footprint2.setDescription("上海美食");
            footprint2.setCityName("上海");
            footprint2.setCategory("美食");
            
            testData.add(footprint1);
            testData.add(footprint2);
            
            // 模拟全部过滤
            allFootprints = testData;
            filterTypeSpinner.setSelection(FILTER_ALL);
            filterFootprints("北京");
            boolean test1 = filteredFootprints.size() == 1 && filteredFootprints.get(0).getId() == 1;
            
            // 模拟分类过滤
            filterTypeSpinner.setSelection(FILTER_CATEGORY);
            filterFootprints("美食");
            boolean test2 = filteredFootprints.size() == 1 && filteredFootprints.get(0).getId() == 2;
            
            return test1 && test2;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}