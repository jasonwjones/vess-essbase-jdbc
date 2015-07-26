package com.saxifrages.vess.h2.models.data;

/**
 * Data from an Essbase cube is modeled in the following way:
 * 
 * The primary key of the table is the compound primary key of each dimension.
 * 
 * Deleting a row is the same as setting data to null
 * Setting data to null is the same as deleting a row
 * There is an indeterminate amount of data in a cube
 * (SELECT COUNT(*) does not return an exact figure)
 * 
 * What should we do about the fact where you do an INSERT
 * but the row already exists? You don't want to worry about
 * violating a PK
 * 
 * While a long integer isn't actually big enough to store every single permutation
 * of data in a sufficiently large cube, it is quite large enough to store every
 * __actually__ loaded piece of data in a cube. In many instances it will be large
 * enough to store every single possible member combination.
 * 
 * @author jasonwjones
 *
 */
public class EssDataReadModel {

}
