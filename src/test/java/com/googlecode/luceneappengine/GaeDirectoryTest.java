package com.googlecode.luceneappengine;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class GaeDirectoryTest extends LocalDatastoreTest {

	@DataPoints
	@SuppressWarnings("deprecation")//try backward compatibility
    public static final Version[] LUCENE_TEST_VERSIONS = new Version[] {
		Version.LUCENE_4_0, Version.LUCENE_4_1, Version.LUCENE_4_1_0,
		Version.LUCENE_4_2, Version.LUCENE_4_2_0, Version.LUCENE_4_2_1,
		Version.LUCENE_4_3, Version.LUCENE_4_3_0, Version.LUCENE_4_3_1,
		Version.LUCENE_4_4, Version.LUCENE_4_4_0,
		Version.LUCENE_4_5, Version.LUCENE_4_5_0, Version.LUCENE_4_5_1,
		Version.LUCENE_4_6, Version.LUCENE_4_6_0, Version.LUCENE_4_6_1,
		Version.LUCENE_4_7, Version.LUCENE_4_7_0, Version.LUCENE_4_7_1, Version.LUCENE_4_7_2,
		Version.LUCENE_4_8, Version.LUCENE_4_8_0, Version.LUCENE_4_8_1, 
		Version.LUCENE_4_9, Version.LUCENE_4_9_0, 
		Version.LUCENE_4_10_0, Version.LUCENE_4_10_1, Version.LUCENE_4_10_2, Version.LUCENE_4_10_3,
		Version.LUCENE_CURRENT,
		Version.LUCENE_5_0_0,
		Version.LATEST
	};
    
    private static IndexWriterConfig config() {
        return GaeLuceneUtil.getIndexWriterConfig(new SimpleAnalyzer());
    }

    @Test
    public void writeAndReadStringInSegment() throws IOException {
        final String input = "Hello World!";
        final String name = "_0.fmt";
		final byte[] bs = input.getBytes();
		
        try (Directory directory = new GaeDirectory()) {
    		try (IndexOutput createOutput = directory.createOutput(name, new IOContext())) {
    		    createOutput.writeBytes(bs, 0, bs.length);
    		}
            
            try (IndexInput openInput = directory.openInput(name, new IOContext())) {
                byte[] bt = new byte[bs.length];
                openInput.readBytes(bt, 0, bt.length);
                assertEquals(new String(bt), input);
            }
        }
    }
    
    @Test
    public void writeAndReadStringInSegment_1Mb() throws IOException {
        final int size = 1024*1024 + 1;
        final String input = RandomStringUtils.random(size + 100, true, true);
        final String name = "_0.fmt";
        
		final byte[] bs = input.getBytes();
		
		assertTrue("Test is not ok! " + bs.length + " <= " + size, bs.length > size);//precondition
		
        try (Directory directory = new GaeDirectory()) {
    		try (IndexOutput createOutput = directory.createOutput(name, new IOContext())) {
    		    createOutput.writeBytes(bs, 0, bs.length);
    		}
            
            try (IndexInput openInput = directory.openInput(name, new IOContext())) {
                byte[] bt = new byte[bs.length];
                openInput.readBytes(bt, 0, bt.length);
                assertEquals(new String(bt), input);
            }
        }
    }
    
    @Theory
    public void writeAndReadDocumentInDirectory(Version luceneVersion) throws IOException {
        final String input = "Hello World!";
        
        try (Directory directory = new GaeDirectory()) {
            
            try (IndexWriter writer = new IndexWriter(directory, config())) {
                Document document = new Document();
                document.add(new Field("title", input, TextField.TYPE_STORED));
                writer.addDocument(document);
            }
            
            try (IndexReader reader = DirectoryReader.open(directory)) {
                Document doc = reader.document(0); 
                assertEquals(doc.get("title"), input);
            }
        }
    }
    
    @Theory
    public void writeAndReadMoreDocumentInDirectory(Version luceneVersion) throws IOException {
        final String input1 = "Hello World!";
        final String input2 = "Hello World!";
        
        try (Directory directory = new GaeDirectory()) {
            
            try (IndexWriter writer = new IndexWriter(directory, config())) {
                Document document = new Document();
                document.add(new Field("title", input1, TextField.TYPE_STORED));
                writer.addDocument(document);
            }
            
            try (IndexWriter writer = new IndexWriter(directory, config())) {
                Document document = new Document();
                document.add(new Field("title", input2, TextField.TYPE_STORED));
                writer.addDocument(document);
            }
            
            try (IndexReader reader = DirectoryReader.open(directory)) {
                Document doc1 = reader.document(0); 
                Document doc2 = reader.document(1); 
                assertEquals(doc1.get("title"), input1);
                assertEquals(doc2.get("title"), input2);
            }
        }
    }

}
