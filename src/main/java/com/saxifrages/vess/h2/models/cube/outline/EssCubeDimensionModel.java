package com.saxifrages.vess.h2.models.cube.outline;

import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.saxifrages.essbase.outline.DimensionReaderOp;
import com.saxifrages.essbase.outline.MemberInfo;
import com.saxifrages.vess.VessConstants;
import com.saxifrages.vess.h2.ColumnBuilder;
import com.saxifrages.vess.h2.EssService;
import com.saxifrages.vess.h2.essresults.EssResultSet;
import com.saxifrages.vess.h2.models.AbstractEssbaseCubeModel;
import com.saxifrages.vess.h2.models.util.ValueBuilder;

public class EssCubeDimensionModel extends AbstractEssbaseCubeModel {
	
	private static final Logger logger = LoggerFactory.getLogger(EssCubeDimensionModel.class);
	
	private DimensionReaderOp readerOp;
	
	private String dimension;
	
	private final static MemberMapper MEMBER_MAPPER = new MemberMapper();
	
	public EssCubeDimensionModel(EssService service, String application, String cube, String dimension) {
		super(service, application, cube);
		this.dimension = dimension;
		this.readerOp = new DimensionReaderOp(this.dimension);
		columns = new ColumnBuilder()
			.addString("NAME")
			.key()
			.notNullable()
			.precision(VessConstants.MAX_MEMBER_NAME_LENGTH)
			.addString("PARENT_NAME")
			.key()
			.notNullable()
			.precision(VessConstants.MAX_MEMBER_NAME_LENGTH)
			.notNullable()
			.addString("STORAGE")
			.precision(40)
			.notNullable()
			.addString("CONSOLIDATION")
			.notNullable()
			.precision(40)
			.addInteger("LEV")
			.notNullable()
			.build();
	} 

	@Override
	public EssResultSet get(IEssCube essCube) throws EssException {
		logger.info("Fetching dimension members for {}.{}, dim {}", application, cube, dimension);
		return mapper(readerOp.run(essCube), MEMBER_MAPPER);
	}

	public static class MemberMapper implements IterMapper<MemberInfo> {
		@Override
		public void convert(MemberInfo mi, Value[] values) throws EssException {
			values[0] = ValueBuilder.stringValue(mi.getName());
			values[1] = ValueBuilder.stringValue(mi.getParentName());
			values[2] = ValueBuilder.stringValue(mi.getStorage());
			values[3] = ValueBuilder.stringValue(mi.getConsolidation());
			values[4] = ValueInt.get(mi.getLevel());
		}
	}
	
}
