<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMArticleLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_article_lng', function (Blueprint $table) {
            $table->increments('ArL_id');
            $table->integer('Ar_id')->comment('Article ID')->unsigned();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned();
            $table->string('ArL_path', 90)->index()->nullable();
            $table->string('ArL_title', 255)->nullable();
            $table->string('ArL_intro', 500)->nullable();
            $table->text('ArL_body')->nullable();
            $table->string('ArL_meta', 300)->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Ar_id')->references('Ar_id')->on('m_article');
            $table->foreign('S_Lng_id')->references('S_Lng_id')->on('s_lang');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_article_lng');
    }
}
