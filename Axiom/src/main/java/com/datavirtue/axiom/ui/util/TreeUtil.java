package com.datavirtue.axiom.ui.util;

import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

public class TreeUtil{
 
    // is path1 descendant of path2
    public static boolean isDescendant(TreePath path1, TreePath path2){
        int count1 = path1.getPathCount();
        int count2 = path2.getPathCount();
        if(count1<=count2)
            return false;
        while(count1!=count2){
            path1 = path1.getParentPath();
            count1--;
        }
        return path1.equals(path2);
    }
 
    public static String getExpansionState(JTree tree, int row){
        TreePath rowPath = tree.getPathForRow(row);
        StringBuffer buf = new StringBuffer();
        int rowCount = tree.getRowCount();
        for(int i=row; i<rowCount; i++){
            TreePath path = tree.getPathForRow(i);
            if(i==row || isDescendant(path, rowPath)){
                if(tree.isExpanded(path))
                    buf.append(","+String.valueOf(i-row));
            }else
                break;
        }
        return buf.toString();
    }
 
    public static void restoreExpanstionState(JTree tree, int row, String expansionState){
        StringTokenizer stok = new StringTokenizer(expansionState, ",");
        while(stok.hasMoreTokens()){
            int token = row + Integer.parseInt(stok.nextToken());
            tree.expandRow(token);
        }
    }


    public static Enumeration saveExpansionState(JTree tree) {

        return tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));

    }


    public static void loadExpansionState(JTree tree, Enumeration enumeration) {

        if (enumeration != null) {

            while (enumeration.hasMoreElements()) {

                TreePath treePath = (TreePath) enumeration.nextElement();

                tree.expandPath(treePath.getParentPath());

            }

        }

    }



}
