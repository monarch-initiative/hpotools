# mondo

The purpose of this app is to extract a list of phenopackets that have narrow and broad parents according to the ClintLR schema.

To run  it, enter the following command

```
mondo
--allppkt allppkt0_1_15/
--clintlr DiseaseIntuitionGroupsTsv.tsv
```

- **allppkt0_1_16** is the path to the all_phenopackets directory that can be downloaded from the
[phenopacket-store](https://github.com/monarch-initiative/phenopacket-store){:target="_blank"} Releases page.
Note that you need to have at least version 0.1.16 of phenopacket-store.
- **DiseaseIntuitionGroupsTsv.tsv** is the file with mappings to narrow and broad Mondo parents (see the ClintLR project for more details).


The result of this command is a new directory called "candidates" that contains a file called ``candidates.tsv``
as well as each of the references phenopackets.