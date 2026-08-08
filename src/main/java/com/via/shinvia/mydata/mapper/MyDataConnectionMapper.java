package com.via.shinvia.mydata.mapper;

import com.via.shinvia.mydata.domain.MyDataConnection;
import com.via.shinvia.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MyDataConnectionMapper {
    int insertMyDataConnection(MyDataConnection myDataConnection);
}
