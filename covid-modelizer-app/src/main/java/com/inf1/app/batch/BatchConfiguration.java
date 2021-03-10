package com.inf1.app.batch;

import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import com.inf1.app.batch.steps.RESTSituationReelleReader;
import com.inf1.app.dto.SituationReelleDTO;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
    private static final String PROPERTY_REST_API_URL = "https://www.data.gouv.fr/fr/datasets/r/d2671c6c-c0eb-4e12-b69a-8e8f87fc224c";
	

	@Bean
	public RestTemplate restTemplate() {
        return new RestTemplate();
    }
	
	@Bean
    ItemReader<SituationReelleDTO> restItemReader(Environment environment,
                                             RestTemplate restTemplate) {
		return new RESTSituationReelleReader(
				environment.getRequiredProperty(PROPERTY_REST_API_URL), 
				restTemplate
		);
    }
	
	/*
    public JsonItemReader reader() throws MalformedURLException {
        return new JsonItemReaderBuilder()
                .jsonObjectReader(new JacksonJsonObjectReader(SituationReelle.class))
                .resource(new UrlResource(
"https://static.data.gouv.fr/resources/donnees-relatives-a-lepidemie-de-covid-19-en-france-vue-densemble/20210309-130127/synthese-fra.json"))
                .name("collectJsonDataItemReader")
                .build();
    }
    */

	@Bean
	JdbcBatchItemWriter<SituationReelleDTO> restItemWriter(DataSource dataSource) {
	  return new JdbcBatchItemWriterBuilder<SituationReelleDTO>()
	    .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
	    .sql("INSERT INTO situation_reelle (cas_confirmes, deces, deces_ehpad, reanimation, hospitalises, "
	    		+ "gueris, date, nouvelles_hospitalisations, nouvelles_reanimations, nouvelles_premieres_injections, "
	    		+ "cumul_premieres_injections, stock_nombre_total_doses, stock_nombre_doses_pfizer, "
	    		+ "stock_nombre_doses_moderna, livraisons_cumul_nombre_total_doses, livraisons_cumul_nombre_doses_pfizer, "
	    		+ "livraisons_cumul_nombre_doses_moderna, total_prises_rendez_vous_semaine, "
	    		+ "prises_rendez_vous_semaine_rang1, prises_rendez_vous_semaine_rang2, stock_ehpad_nombre_doses_pfizer) "
	    		+ "VALUES (:casConfirmes, :deces, :decesEhpad, :reanimation, :hospitalises, :gueris, :date, :nouvellesHospitalisations, "
	    		+ ":nouvellesReanimations, :nouvellesPremieresInjections, :cumulPremieresInjections, "
	    		+ ":stockNombreTotalDoses, :stockNombreDosesPfizer, :stockNombreDosesModerna, "
	    		+ ":livraisonsCumulNombreTotalDoses, :livraisonsCumulNombreDosesPfizer, "
	    		+ ":livraisonsCumulNombreDosesModerna, :totalPrisesRendezVousSemaine, :prisesRendezVousSemaineRang1, "
	    		+ ":prisesRendezVousSemaineRang2, :stockEhpadNombreDosesPfizer)")
	    .dataSource(dataSource)
	    .build();
	}
	
	@Bean
	Job importUserJob(JobBuilderFactory jobBuilderFactory,
			@Qualifier("restSituationReelleStep") Step step1) {
	  return jobBuilderFactory.get("collectDataJob")
	    .incrementer(new RunIdIncrementer())
	    .flow(step1)
	    .end()
	    .build();
	}

	@Bean
	Step restSituationReelleStep(ItemReader<SituationReelleDTO> restItemReader, 
			JdbcBatchItemWriter<SituationReelleDTO> restItemWriter,
			StepBuilderFactory stepBuilderFactory) {
	  return stepBuilderFactory.get("restSituationReelleStep")
	    .<SituationReelleDTO, SituationReelleDTO> chunk(1)
	    .reader(restItemReader)
	    .writer(restItemWriter)
	    .build();
	}
	
}
