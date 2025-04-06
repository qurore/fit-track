package com.github.qurore.fittrack.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.qurore.fittrack.R;

import java.util.List;
import java.util.Map;

public class ExerciseListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> categories;
    private Map<String, List<String>> exercises;

    public ExerciseListAdapter(Context context, List<String> categories, Map<String, List<String>> exercises) {
        this.context = context;
        this.categories = categories;
        this.exercises = exercises;
    }

    @Override
    public int getGroupCount() {
        return categories.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return exercises.get(categories.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return categories.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return exercises.get(categories.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_exercise_category, parent, false);
        }

        TextView categoryName = convertView.findViewById(R.id.categoryName);
        ImageView expandIcon = convertView.findViewById(R.id.expandIcon);
        ImageView categoryIcon = convertView.findViewById(R.id.categoryIcon);

        categoryName.setText(categories.get(groupPosition));
        
        // Animate chevron rotation
        float targetRotation = isExpanded ? 90 : 0;
        if (expandIcon.getRotation() != targetRotation) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(expandIcon, "rotation", expandIcon.getRotation(), targetRotation);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        } else {
            expandIcon.setRotation(targetRotation);
        }
        
        // Set category icon based on category name
        setCategoryIcon(categoryIcon, categories.get(groupPosition));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_exercise, parent, false);
        }

        TextView exerciseName = convertView.findViewById(R.id.exerciseName);
        exerciseName.setText((String) getChild(groupPosition, childPosition));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private void setCategoryIcon(ImageView imageView, String category) {
        // Set appropriate icon based on category
        switch (category.toLowerCase()) {
            case "chest":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case "back":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case "legs":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case "shoulders":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case "arms":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case "core":
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
            default:
                imageView.setImageResource(android.R.drawable.ic_menu_compass);
                break;
        }
    }
} 