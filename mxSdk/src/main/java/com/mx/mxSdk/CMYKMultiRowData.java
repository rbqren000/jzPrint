package com.mx.mxSdk;

import static com.mx.mxSdk.RowLayoutDirection.RowLayoutDirectionVertical;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class CMYKMultiRowData implements Parcelable {
    /** 多拼图或者单张大图裁切形成的连续图生成的打印数据 */
    private final ArrayList<CMYKRowData> rowDataArr;
    /** 多拼图或者单张大图裁切形成的连续图生成的预览图的存储地址 */
    private final ArrayList<String> imagePaths;
    /** 缩略图生成的地址 thumbPath有可能为null，
     * 是否将缩略图生成预览图需要看MultiRowDataFactory的bitmap2MultiRowData方法传入的thumbToSimulation的值
     * thumbToSimulation为true则生成预览图，thumbToSimulation为false则不将缩略图生成预览图*/
    private final String thumbPath;
    /** 是否压缩打印数据 有损压缩 */
    private final boolean compress;
    /** 多图的排布方向，该值和MultiRowImage的rowLayoutDirection保持一致 */
    private final RowLayoutDirection rowLayoutDirection;

    private CMYKMultiRowData(@NonNull ArrayList<CMYKRowData> rowDataArr, @NonNull ArrayList<String> imagePaths, String thumbPath, boolean compress) {
        this.rowDataArr = rowDataArr;
        this.imagePaths = imagePaths;
        this.thumbPath = thumbPath;
        this.compress = compress;
        this.rowLayoutDirection = RowLayoutDirectionVertical;
    }

    private CMYKMultiRowData(@NonNull ArrayList<CMYKRowData> rowDataArr, @NonNull ArrayList<String> imagePaths, String thumbPath, boolean compress, RowLayoutDirection rowLayoutDirection) {
        this.rowDataArr = rowDataArr;
        this.imagePaths = imagePaths;
        this.thumbPath = thumbPath;
        this.compress = compress;
        this.rowLayoutDirection = rowLayoutDirection;
    }

    // 添加工厂方法
    public static CMYKMultiRowData createInstance(@NonNull ArrayList<CMYKRowData> rowDataArr, @NonNull ArrayList<String> imagePaths, String thumbPath, boolean compress) {
        return new CMYKMultiRowData(rowDataArr, imagePaths, thumbPath, compress);
    }

    public static CMYKMultiRowData createInstance(@NonNull ArrayList<CMYKRowData> rowDataArr, @NonNull ArrayList<String> imagePaths, String thumbPath, boolean compress, RowLayoutDirection rowLayoutDirection) {
        return new CMYKMultiRowData(rowDataArr, imagePaths, thumbPath, compress,rowLayoutDirection);
    }

    protected CMYKMultiRowData(Parcel in) {
        rowDataArr = in.createTypedArrayList(CMYKRowData.CREATOR);
        imagePaths = in.createStringArrayList();
        thumbPath = in.readString();
        compress = in.readByte() != 0;
        String directionName = in.readString();
        rowLayoutDirection = RowLayoutDirection.valueOf(directionName);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(rowDataArr);
        dest.writeStringList(imagePaths);
        dest.writeString(thumbPath);
        dest.writeByte((byte)(compress?1:0));
        dest.writeString(rowLayoutDirection.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CMYKMultiRowData> CREATOR = new Creator<CMYKMultiRowData>() {
        @Override
        public CMYKMultiRowData createFromParcel(Parcel in) {
            return new CMYKMultiRowData(in);
        }

        @Override
        public CMYKMultiRowData[] newArray(int size) {
            return new CMYKMultiRowData[size];
        }
    };

    public ArrayList<CMYKRowData> getRowDataArr() {
        return rowDataArr;
    }

    public ArrayList<String> getImagePaths() {
        return imagePaths;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public boolean isCompress() {
        return compress;
    }
    
    public int compressValue(){
        return this.compress?1:0;
    }

    public RowLayoutDirection getRowLayoutDirection() {
        return rowLayoutDirection;
    }

    public int totalDataLength(){
        int totalDataLength = 0;
        for (CMYKRowData rowData : rowDataArr){
            totalDataLength = totalDataLength + rowData.totalDataLength();
        }
        return totalDataLength;
    }

    public int totalPacketCount(int usefulDataLen){

        int totalPacketCount = 0;
        for (CMYKRowData rowData : rowDataArr){
            totalPacketCount = totalPacketCount + rowData.totalPacketCount(usefulDataLen);
        }
        return totalPacketCount;
    }

    public Boolean hasData(){
        return totalDataLength() > 0;
    }

    public int totalRowCount(){
        return rowDataArr.size();
    }

    public CMYKRowData rowDataWithRowIndex(int rowIndex){
        if (rowIndex <0|| rowIndex >= rowDataArr.size()){
            return null;
        }
        return rowDataArr.get(rowIndex);
    }

    @NonNull
    @Override
    public String toString() {
        return "CMYKMultiRowData{" +
                "rowDataArr=" + rowDataArr +
                ", imagePaths=" + imagePaths +
                ", thumbPath='" + thumbPath + '\'' +
                ", compress=" + compress +
                ", rowLayoutDirection=" + rowLayoutDirection +
                '}';
    }
}
