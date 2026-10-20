package com.liugh.rcs.service;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.liugh.entity.RcsFile;
import com.liugh.mapper.RcsFileMapper;

public class RcsFileServiceTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void deletesOwnedLocalFileAndMetadata() throws Exception {
        RcsFileMapper mapper = mock(RcsFileMapper.class);
        RcsFileService service = new RcsFileService(mapper,
                "https://platform.example.com/api/v1/",
                temp.getRoot().getAbsolutePath());
        RcsFile stored = new RcsFile();
        stored.setId(7L);
        stored.setSsid(100);
        stored.setUid("file_1");
        when(mapper.selectList(any()))
                .thenReturn(Collections.singletonList(stored));
        File directory = new File(temp.getRoot(), "rcs");
        directory.mkdirs();
        File local = new File(directory, "file_1");
        Files.write(local.toPath(), new byte[] {1, 2, 3});

        service.deleteOwned(100, "file_1");

        assertFalse(local.exists());
        verify(mapper).deleteById(7L);
    }

    @Test
    public void rejectsRawFilePathTraversal() throws Exception {
        RcsFileService service = new RcsFileService(mock(RcsFileMapper.class),
                "https://platform.example.com/api/v1/",
                temp.getRoot().getAbsolutePath());

        org.junit.Assert.assertNull(service.loadRaw("../../secret"));
    }
}
