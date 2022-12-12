<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMgalTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_gallery', function (Blueprint $table) {
            $table->increments('ArG_id');
            $table->integer('Ar_id')->comment('Article ID')->unsigned();

            $table->string('ArG_file', 150)->nullable();
            $table->string('ArG_name', 250)->nullable();
            $table->string('ArG_size', 20)->nullable();
            $table->string('ArG_type', 20)->nullable();



            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Ar_id')->references('Ar_id')->on('m_article');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_gallery');
    }
}
