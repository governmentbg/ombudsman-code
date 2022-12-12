<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMFilesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_files', function (Blueprint $table) {
            $table->increments('ArF_id');
            $table->integer('ArL_id')->comment('Article Lng ID')->unsigned();

            $table->string('ArF_file', 150)->nullable();
            $table->string('ArF_name', 250)->nullable();
            $table->string('ArF_size', 20)->nullable();
            $table->string('ArF_type', 20)->nullable();
            $table->string('ArF_desc', 500)->nullable();


            $table->timestamps();
            $table->softDeletes();

            $table->foreign('ArL_id')->references('ArL_id')->on('m_article_lng');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_files');
    }
}
