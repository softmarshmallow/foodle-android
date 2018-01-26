/*
 *    Copyright (C) 2016 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.softmarshmallow.foodle.Views.Test.DragListTest;

import android.Manifest;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;
import com.softmarshmallow.foodle.R;
import com.softmarshmallow.foodle.Views.PhotoSelectorView.PhotoSelectorActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.OnClick;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

/*
 * This example shows very very minimal implementation of draggable feature.
 * Please refer to other examples for more advanced usages. Thanks!
 */
public class MinimalDraggableExampleActivity extends AppCompatActivity
{
    LinearLayout llBottomSheet;
    MyAdapter adapter = new MyAdapter();
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    static BottomSheetDialog mBottomSheetDialog;
    RecyclerView recyclerView;

    public static void selectImage() {

        mBottomSheetDialog.show();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mBottomSheetDialog.hide();
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                adapter.mItems.add(new MyItem(1,"Camera",photo));
            } else if (requestCode == 2) {try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                addPhoto(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
               // Toast.makeText("", "Something went wrong", Toast.LENGTH_LONG).show();
            }

            }else {
                //Toast.makeText("post", "You haven't picked Image",Toast.LENGTH_LONG).show();
            }

        }
    }

    private void addPhoto(Bitmap bit) {
        adapter.mItems.add(new MyItem(3,"Main", bit));
        adapter.notifyDataSetChanged();

    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();//\
            } else {

                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_minimal);
        mBottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = this.getLayoutInflater().inflate(R.layout.fragment_photoselecter_bottom, null);
        mBottomSheetDialog.setContentView(sheetView);
        LinearLayout edit = (LinearLayout) sheetView.findViewById(R.id.fragment_history_bottom_sheet_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_REQUEST_CODE);
                    // Edit code here;
                }else{
                    Intent cameraIntent = new Intent(ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);

                }
            }
        });

        LinearLayout delete = (LinearLayout) sheetView.findViewById(R.id.fragment_history_bottom_sheet_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Delete code here;

                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 2);
            }
        });

        recyclerView = findViewById(R.id.recycler_view);

        RecyclerViewDragDropManager dragMgr = new RecyclerViewDragDropManager();
    
        dragMgr.setInitiateOnMove(false);
        dragMgr.setInitiateOnLongPress(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(dragMgr.createWrappedAdapter(adapter));
        dragMgr.attachRecyclerView(recyclerView);

        Snackbar.make(findViewById(R.id.container), "TIP: Long press item to initiate Drag & Drop action!", Snackbar.LENGTH_LONG).show();

        Drawable vectorDrawable = ResourcesCompat.getDrawable(this.getResources(),
                R.drawable.plus, null);
        Bitmap myLogo = ((BitmapDrawable) vectorDrawable).getBitmap();

        adapter.mItems.add(new MyItem(0,"Plus", myLogo));
        Intent intent = getIntent();
        Bitmap bitmap = (Bitmap) intent.getParcelableExtra("MainBitmap");
        addPhoto(bitmap);
        selectImage();
    }

    static class MyItem {
        public final long id;
        public final String text;
        public Bitmap bitmap;

        public MyItem(long id, String text) {
            this.id = id;
            this.text = text;
        }
        public MyItem(long id, String text, Bitmap bitmap) {
            this.id = id;
            this.text = text;
            this.bitmap = bitmap;
        }
    }

    static class MyViewHolder extends AbstractDraggableItemViewHolder {
        TextView textView;
        ImageView imageView;

        public MyViewHolder(final View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            imageView = itemView.findViewById(R.id.ImageSelecterImage);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (textView.getText().equals("Plus"))
                        MinimalDraggableExampleActivity.selectImage();
                    else
                        Log.d("TAG", "instance initializer: Sex");

                }
            });
        }


    }

    static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> implements DraggableItemAdapter<MyViewHolder> {
        List<MyItem> mItems;

        public MyAdapter() {
            setHasStableIds(true); // this is required for D&D feature.

            mItems = new ArrayList<>();
//            for (int i = 0; i < 20; i++) {
//                mItems.add(new MyItem(i, "Item " + i, ));
//            }
        }
        @Override
        public long getItemId(int position) {
            return mItems.get(position).id; // need to return stable (= not change even after reordered) value
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_minimal, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            MyItem item = mItems.get(position);
            holder.textView.setText(item.text);
            holder.imageView.setImageBitmap(item.bitmap);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            MyItem movedItem = mItems.remove(fromPosition);
            mItems.add(toPosition, movedItem);
        }

        @Override
        public boolean onCheckCanStartDrag(MyViewHolder holder, int position, int x, int y) {
            return true;
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(MyViewHolder holder, int position) {
            return null;
        }

        @Override
        public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
            return true;
        }

        @Override
        public void onItemDragStarted(int position) {
        }

        @Override
        public void onItemDragFinished(int fromPosition, int toPosition, boolean result) {
        }
    }
}
