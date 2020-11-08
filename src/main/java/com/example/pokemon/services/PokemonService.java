package com.example.pokemon.services;


import com.example.pokemon.entities.Pokemon;
import com.example.pokemon.repositories.PokemonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class PokemonService {

    @Autowired
    private PokemonRepository pokemonRepository;
    @Autowired
    private PokemonConsumerService pokemonConsumerService;


    @Cacheable(value = "pokemonCache", key = "#name")
    public List<Pokemon> findAll(String name, String weight){
        var pokemons = pokemonRepository.findByName(name);
        if(pokemons.isEmpty()){
            var PokemonDto = pokemonConsumerService.search(name, weight);
            if(PokemonDto != null){
                var pokemon = new Pokemon(PokemonDto.getName(),PokemonDto.getHeight(),PokemonDto.getWeight(), PokemonDto.getBaseExperience(), PokemonDto.getLocationEncounter(), PokemonDto.getTypes(), PokemonDto.getAbilities(), PokemonDto.getGames());
                pokemons.add(this.save(pokemon));
            }
        }
        return pokemons;
    }

    @Cacheable(value = "pokemonCache", key = "#name")
    public List<Pokemon> findPokemonByName(String name, String type){
        if (pokemonRepository.findAll().isEmpty()){
            pokemonConsumerService.getAllPokemonsFromApi();
        }
        if (name == null && type == null){
            return pokemonRepository.findAll();
        }
        if (name != null && type != null){
            var listFromDb = this.pokemonInDBCheckWithNameAndType(name, type);
            if (!listFromDb.isEmpty()){
                return listFromDb;
            }
        }else if(name != null){
            var listFromDb = this.pokemonInDBCheckWithName(name);
            if (!listFromDb.isEmpty()){
                return listFromDb;
            }
        }

        var pokemonData = pokemonConsumerService.search(name, type);

        return (List<Pokemon>) pokemonData;
    }

    private List<Pokemon> pokemonInDBCheckWithNameAndType(String name, String type){
        var pokemonsListedInDB = pokemonRepository.findAll();
        pokemonsListedInDB = pokemonsListedInDB.stream()
                .filter(pokemon -> pokemon.getName().toLowerCase().contains(name))
               // .filter(pokemon -> pokemon.getTypes().stream().anyMatch(pokeType -> pokeType.getType().name.toLowerCase().contains(type)))
                .collect(Collectors.toList());
        if(pokemonsListedInDB.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No pokemon found by name: %s and type: %s", name, type));
        }
        return pokemonsListedInDB;

    }

    private List<Pokemon> pokemonInDBCheckWithName(String name){
        var pokemonsListedInDB = pokemonRepository.findAll();
        pokemonsListedInDB = pokemonsListedInDB.stream()
                .filter(pokemon -> pokemon.getName().toLowerCase().contains(name))
                .collect(Collectors.toList());
        if(pokemonsListedInDB.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No pokemon found by name: %s", name));
        }
        return pokemonsListedInDB;
    }


    public Pokemon findById(String id){
        return pokemonRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Kunde inte hitta pokemonen.."));
    }

    @CachePut(value = "pokemonCache", key = "#result.id")
    public Pokemon save(Pokemon pokemon){
        return pokemonRepository.save(pokemon);
    }

    @CachePut(value = "pokemonCache", key = "id")
    public void update(String id, Pokemon pokemon){
        if(!pokemonRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kunde inte hitta pokemonen..");
        }
        pokemon.setPokemonID(id);
        pokemonRepository.save(pokemon);
    }

    @CacheEvict(value = "pokemonCache", allEntries = true)
    public void delete(String id){
        if(!pokemonRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kunde inte hitta pokemonen..");
        }
        pokemonRepository.deleteById(id);
    }

}
