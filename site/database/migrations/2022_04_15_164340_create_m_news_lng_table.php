<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMNewsLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_news_lng', function (Blueprint $table) {
            $table->increments('MnL_id');
            $table->integer('Mn_id')->comment('Mnticle ID')->unsigned();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned();
            $table->string('MnL_path', 90)->index()->nullable();
            $table->string('MnL_title', 255)->nullable();
            $table->string('MnL_intro', 500)->nullable();
            $table->text('MnL_body')->nullable();
            $table->string('MnL_meta', 300)->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Mn_id')->references('Mn_id')->on('m_news');
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
        Schema::dropIfExists('m_news_lng');
    }
}
